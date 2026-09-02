import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {afterRenderEffect, Component, computed, ElementRef, inject, OnDestroy, signal, Signal, untracked, viewChild, WritableSignal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {HelpSet} from './api/HelpSet';
import {ConfigService} from './config.service';
import {ErrorResponseComponent} from './error-response.component';
import {FooterComponent} from './footer.component';
import {MenuComponent} from './menu.component';
import {HelpPagesLoader} from './misc/HelpPagesLoader';
import {NavItem} from './misc/NavItem';
import * as Utils from './misc/Utils';
import {ProgressSpinnerComponent} from './progress-spinner.component';
import {TocListComponent} from './toc-list.component';

@Component({
   selector: 'marec-help-set',
   templateUrl: './help-set.component.html',
   styleUrl: './help-set.component.scss',
   imports: [
      RouterOutlet, DatePipe,
      ErrorResponseComponent, FooterComponent, MenuComponent, ProgressSpinnerComponent, TocListComponent,
   ],
})
export class HelpSetComponent implements OnDestroy {
   private readonly configService: ConfigService = inject(ConfigService);

   private readonly helpContentEl: Signal<ElementRef<HTMLElement>> = viewChild.required('helpContent');
   private readonly helpPagesLoader: WritableSignal<HelpPagesLoader | undefined> = signal(undefined);
   protected readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);
   protected readonly helpSet: WritableSignal<HelpSet | undefined> = signal(undefined);
   protected readonly loading: Signal<boolean> = computed(() => !!this.helpPagesLoader());
   protected readonly fraction: Signal<number> = computed(() => this.helpPagesLoader()?.fraction() ?? 0);

   constructor() {
      this.configService.routePrefix = '/set';
      afterRenderEffect(() => {
         const navItem = this.configService.navigation();
         if (navItem) {
            untracked(() => this.navigateTo(navItem));
         }
      });
   }

   ngOnDestroy(): void {
      this.helpPagesLoader()?.cancel();
      this.configService.currentHelpSet = undefined;
   }

   private navigateTo(navItem: NavItem): void {
      const helpSet = navItem.helpSet;
      this.configService.currentHelpSet = helpSet;
      if (this.helpSet()?.id === helpSet.id) {
         Utils.scrollToNavItem(this.configService, navItem);
      } else {
         this.helpSet.set(undefined);
         const helpContent = this.helpContentEl().nativeElement;
         helpContent.innerHTML = '';
         this.helpPagesLoader()?.cancel();
         const helpPagesLoader = new HelpPagesLoader(this.configService, helpSet);
         this.helpPagesLoader.set(helpPagesLoader);
         helpPagesLoader.helpPages.then(helpPages => {
            this.helpSet.set(helpSet);
            Utils.addHelpPages(this.configService, helpContent, helpSet, helpPages);
            setTimeout(() => Utils.scrollToNavItem(this.configService, navItem));
            this.helpPagesLoader.set(undefined);
            this.errorResponse.set(undefined);
         }).catch(error => {
            this.helpPagesLoader.set(undefined);
            this.errorResponse.set(error);
            console.error(error);
         });
      }
   }
}
