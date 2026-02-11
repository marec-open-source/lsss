import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {AfterViewInit, ChangeDetectionStrategy, Component, computed, ElementRef, inject, OnDestroy, signal, Signal, viewChild, WritableSignal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {Subscription} from 'rxjs';
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
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-help-set',
   templateUrl: './help-set.component.html',
   styleUrl: './help-set.component.scss',
   imports: [
      RouterOutlet, DatePipe,
      ErrorResponseComponent, FooterComponent, MenuComponent, ProgressSpinnerComponent, TocListComponent,
   ],
})
export class HelpSetComponent implements AfterViewInit, OnDestroy {
   private readonly configService: ConfigService = inject(ConfigService);

   private readonly helpContentEl: Signal<ElementRef<HTMLElement>> = viewChild.required('helpContent');
   private navigationSubscription?: Subscription;
   private readonly helpPagesLoader: WritableSignal<HelpPagesLoader | undefined> = signal(undefined);
   protected readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);
   protected readonly helpSet: WritableSignal<HelpSet | undefined> = signal(undefined);
   protected readonly loading: Signal<boolean> = computed(() => !!this.helpPagesLoader());
   protected readonly fraction: Signal<number> = computed(() => this.helpPagesLoader()?.fraction() ?? 0);

   constructor() {
      this.configService.routePrefix = '/set';
   }

   ngAfterViewInit(): void {
      setTimeout(() => this.startNavigationSubscription());
   }

   ngOnDestroy(): void {
      this.navigationSubscription?.unsubscribe();
      this.helpPagesLoader()?.cancel();
      this.configService.currentHelpSet = undefined;
   }

   private startNavigationSubscription(): void {
      this.navigationSubscription = this.configService.navigation.subscribe(navItem => {
         if (navItem) {
            this.navigateTo(navItem);
         }
      });
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
         helpPagesLoader.helpPages.subscribe({
            next: helpPages => {
               this.helpSet.set(helpSet);
               Utils.addHelpPages(this.configService, helpContent, helpSet, helpPages);
               setTimeout(() => Utils.scrollToNavItem(this.configService, navItem));
               this.helpPagesLoader.set(undefined);
               this.errorResponse.set(undefined);
            },
            error: error => {
               this.helpPagesLoader.set(undefined);
               this.errorResponse.set(error);
               console.log(error);
            }
         });
      }
   }
}
