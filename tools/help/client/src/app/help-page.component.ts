import {DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {
   afterNextRender,
   afterRenderEffect,
   Component,
   computed,
   effect,
   ElementRef,
   HostListener,
   inject,
   OnDestroy,
   signal,
   Signal,
   untracked,
   viewChild,
   WritableSignal
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatTree, MatTreeModule} from '@angular/material/tree';
import {RouterOutlet} from '@angular/router';
import {Subscription} from 'rxjs';
import {HelpSet} from './api/HelpSet';
import {TocItem} from './api/TocItem';
import {ConfigService} from './config.service';
import {ErrorResponseComponent} from './error-response.component';
import {FooterComponent} from './footer.component';
import {MenuComponent} from './menu.component';
import {HelpPage} from './misc/HelpPage';
import {NavItem} from './misc/NavItem';
import {SearchResult} from './misc/SearchResult';
import * as Utils from './misc/Utils';
import {ProgressSpinnerComponent} from './progress-spinner.component';
import {SearchService} from './search.service';

@Component({
   selector: 'marec-help-page',
   templateUrl: './help-page.component.html',
   styleUrl: './help-page.component.scss',
   imports: [
      RouterOutlet, DecimalPipe, FormsModule,
      MatButtonModule, MatFormFieldModule, MatInputModule, MatTreeModule,
      ErrorResponseComponent, FooterComponent, MenuComponent, ProgressSpinnerComponent,
   ],
})
export class HelpPageComponent implements OnDestroy {
   protected readonly configService: ConfigService = inject(ConfigService);
   protected readonly searchService: SearchService = inject(SearchService);

   private readonly tree: Signal<MatTree<TocItem>> = viewChild.required('tree');
   protected readonly childrenAccessor: (tocItem: TocItem) => TocItem[] = tocItem => tocItem.items ?? [];
   protected readonly toc: TocItem[];

   protected readonly searchText: WritableSignal<string> = signal('');
   protected readonly searchResults: Signal<SearchResult[]> = computed(() => {
      return this.searchService.searchFunction()(this.searchText());
   });

   private readonly helpContentEl: Signal<ElementRef<HTMLElement>> = viewChild.required('helpContent');

   private skipScrollToc: boolean = false;
   protected readonly selectedTocItem: WritableSignal<TocItem | undefined> = signal(undefined);
   protected readonly leftVisible: WritableSignal<boolean> = signal(false);

   protected readonly loading: WritableSignal<boolean> = signal(false);
   protected readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);
   private currentlyLoadedPage: {helpSet?: HelpSet, pageId?: string} = {};

   private httpSubscription?: Subscription;

   private touchStart?: { time: number, x: number, y: number };

   constructor() {
      this.configService.routePrefix = '/page';
      this.toc = this.configService.config.helpSets.flatMap(helpSet => helpSet.toc);
      effect(() => {
         if (!this.searchText()) {
            const selectedTocItem = untracked(this.selectedTocItem);
            if (selectedTocItem) {
               this.scrollToc(selectedTocItem);
            }
         }
      });
      afterRenderEffect(() => {
         const navItem = this.configService.navigation();
         if (navItem) {
            untracked(() => this.navigateTo(navItem));
         }
      });
      afterNextRender(() => {
         this.toc.forEach(tocItem => this.tree().expand(tocItem));
      });
   }

   ngOnDestroy(): void {
      this.httpSubscription?.unsubscribe();
   }

   private navigateTo(navItem: NavItem): void {
      const skipScrollToc = this.skipScrollToc;
      this.skipScrollToc = false;
      const tocItem = this.configService.navItemToTocItem(navItem);
      this.selectedTocItem.set(tocItem);
      if (!skipScrollToc) {
         this.scrollToc(tocItem);
      }
      if (this.currentlyLoadedPage.helpSet === navItem.helpSet && this.currentlyLoadedPage.pageId === navItem.pageId) {
         Utils.scrollToNavItem(this.configService, navItem);
      } else {
         this.currentlyLoadedPage = {};
         this.loading.set(true);
         const helpContent = this.helpContentEl().nativeElement;
         helpContent.innerHTML = '';
         const url = this.configService.navItemToUrl(navItem);
         this.httpSubscription?.unsubscribe();
         this.httpSubscription = this.configService.httpGetText(url).subscribe({
            next: data => {
               Utils.addHelpPages(this.configService, helpContent, navItem.helpSet, [new HelpPage(navItem.pageId, data)]);
               helpContent.querySelectorAll('pre').forEach(pre => {
                  pre.onscroll = () => this.onScroll();
               });
               Utils.scrollToNavItem(this.configService, navItem);
               if (!skipScrollToc) {
                  this.scrollToc(tocItem); // Again, since scrollToNavItem might stop smooth toc scrolling.
               }
               this.currentlyLoadedPage = {helpSet: navItem.helpSet, pageId: navItem.pageId};
               this.loading.set(false);
               this.errorResponse.set(undefined);
            },
            error: error => {
               this.loading.set(false);
               this.errorResponse.set(error);
               console.error(error);
            }
         });
      }
   }

   hasChild(_: number, tocItem: TocItem): boolean {
      return !!tocItem.items;
   }

   onSearchFocus(): void {
      this.searchService.init();
   }

   tocId(tocItem: TocItem): string {
      const navItem = tocItem.navItem;
      return `toc/${navItem.helpSet.id}/${navItem.pageId}/${navItem.anchor}`;
   }

   tocLink(tocItem: TocItem): string {
      return this.configService.navItemToLink(tocItem.navItem);
   }

   tocImgSrc(tocItem: TocItem): string {
      return tocItem.navItem.helpSet.imgSrc;
   }

   tocClick(tocItem: TocItem): void {
      const tree = this.tree();
      if (this.selectedTocItem() === tocItem && tree.isExpanded(tocItem)) {
         tree.collapse(tocItem);
      } else {
         this.expand(tocItem);
      }
      this.skipScrollToc = this.selectedTocItem() !== tocItem;
      this.selectedTocItem.set(tocItem);
      this.leftVisible.set(false);
   }

   searchResultClick(result: SearchResult): void {
      this.selectedTocItem.set(result.tocItem);
      this.leftVisible.set(false);
   }

   private scrollToc(tocItem: TocItem): void {
      this.expand(tocItem);
      setTimeout(() => {
         const element = document.getElementById(this.tocId(tocItem));
         element?.scrollIntoView({behavior: 'smooth'});
      });
   }

   private expand(tocItem: TocItem | undefined): void {
      while (tocItem) {
         this.tree().expand(tocItem);
         tocItem = tocItem.parent;
      }
   }

   onScroll(): void {
      this.touchStart = undefined;
   }

   @HostListener('touchstart', ['$event'])
   // eslint-disable-next-line @typescript-eslint/no-explicit-any
   onTouchStart(event: any): void { // Some browsers do not support TouchEvent.
      if (this.touchStart || event.changedTouches.length > 1) {
         this.touchStart = undefined;
         return;
      }
      const touch = event.changedTouches[0];
      this.touchStart = {time: Date.now(), x: touch.pageX, y: touch.pageY};
   }

   @HostListener('touchend', ['$event'])
   // eslint-disable-next-line @typescript-eslint/no-explicit-any
   onTouchEnd(event: any): void { // Some browsers do not support TouchEvent.
      const touch = event.changedTouches[0];
      const touchStart = this.touchStart;
      if (!touchStart) {
         return;
      }
      const dt = Date.now() - touchStart.time;
      const dx = touch.pageX - touchStart.x;
      const dy = touch.pageY - touchStart.y;
      if (dt < 1000 && Math.abs(dx) > Math.max(5, Math.abs(3 * dy))) {
         this.leftVisible.set(dx > 0);
      }
      this.touchStart = undefined;
   }
}
