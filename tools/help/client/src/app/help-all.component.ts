import {HttpErrorResponse} from '@angular/common/http';
import {AfterViewInit, ChangeDetectionStrategy, Component, computed, ElementRef, inject, OnDestroy, signal, Signal, viewChild, WritableSignal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {Subscription} from 'rxjs';
import {HelpSet} from './api/HelpSet';
import {ConfigService} from './config.service';
import {ErrorResponseComponent} from './error-response.component';
import {FooterComponent} from './footer.component';
import {MenuComponent} from './menu.component';
import {HelpPage} from './misc/HelpPage';
import {HelpPagesLoader} from './misc/HelpPagesLoader';
import {NavItem} from './misc/NavItem';
import * as Utils from './misc/Utils';
import {ProgressSpinnerComponent} from './progress-spinner.component';

@Component({
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-help-all',
   templateUrl: './help-all.component.html',
   styleUrl: './help-all.component.scss',
   imports: [
      RouterOutlet,
      ErrorResponseComponent, FooterComponent, MenuComponent, ProgressSpinnerComponent,
   ],
})
export class HelpAllComponent implements AfterViewInit, OnDestroy {
   protected readonly configService: ConfigService = inject(ConfigService);

   protected readonly helpContentEl: Signal<ElementRef<HTMLElement>> = viewChild.required('helpContent');
   private navigationSubscription?: Subscription;
   private readonly helpPagesLoader: WritableSignal<HelpPagesLoader | undefined> = signal(undefined);
   protected readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);
   private readonly pagesLoaded: WritableSignal<number> = signal(0);
   protected readonly loading: Signal<boolean> = computed(() => !!this.helpPagesLoader());
   protected readonly fraction: Signal<number>;

   constructor() {
      this.configService.routePrefix = '/all';
      const totalPages = this.configService.config.helpSets.reduce((sum, helpSet) => sum + helpSet.pageIds.length, 0);
      this.fraction = computed(() => (this.pagesLoaded() + (this.helpPagesLoader()?.pagesLoaded() ?? 0)) / totalPages);
   }

   ngAfterViewInit(): void {
      setTimeout(() => this.loadData([]));
   }

   ngOnDestroy(): void {
      this.navigationSubscription?.unsubscribe();
      this.cancelHelpPagesLoader();
   }

   private startNavigationSubscription(): void {
      this.navigationSubscription = this.configService.navigation.subscribe(navItem => {
         if (navItem) {
            this.navigateTo(navItem);
         }
      });
   }

   private cancelHelpPagesLoader(): void {
      this.helpPagesLoader()?.cancel();
   }

   private navigateTo(navItem: NavItem): void {
      Utils.scrollToNavItem(this.configService, navItem);
   }

   private loadData(dataList: { helpSet: HelpSet, pages: HelpPage[] }[]): void {
      const helpSet = this.configService.config.helpSets[dataList.length];
      if (!helpSet) {
         this.helpPagesLoader.set(undefined);
         const helpContent = this.helpContentEl().nativeElement;
         dataList.forEach(data => {
            Utils.addHelpPages(this.configService, helpContent, data.helpSet, data.pages);
         });
         this.startNavigationSubscription();
         return;
      }
      const helpPagesLoader = new HelpPagesLoader(this.configService, helpSet);
      this.helpPagesLoader.set(helpPagesLoader);
      helpPagesLoader.helpPages.subscribe({
         next: pages => {
            dataList.push({helpSet, pages});
            this.pagesLoaded.update(n => n + pages.length);
            this.loadData(dataList);
         },
         error: error => {
            this.helpPagesLoader.set(undefined);
            this.errorResponse.set(error);
            console.log(error);
         }
      });
   }
}
