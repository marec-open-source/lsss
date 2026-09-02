import {computed, Signal, signal, WritableSignal} from '@angular/core';
import {Subscription} from 'rxjs';
import {HelpSet} from '../api/HelpSet';
import {ConfigService} from '../config.service';
import {HelpPage} from './HelpPage';
import {NavItem} from './NavItem';

export class HelpPagesLoader {
   readonly helpPages: Promise<HelpPage[]>;
   readonly pagesLoaded: WritableSignal<number> = signal(0);
   readonly fraction: Signal<number>;

   private readonly httpSubscriptions: Subscription[] = [];

   constructor(configService: ConfigService, helpSet: HelpSet) {
      const helpPages: HelpPage[] = [];
      this.helpPages = new Promise<HelpPage[]>((resolve, reject) => {
         helpSet.pageIds.forEach((pageId, pageIndex) => {
            const url = configService.navItemToUrl(new NavItem(helpSet, pageId));
            this.httpSubscriptions.push(configService.httpGetText(url).subscribe({
               next: data => {
                  helpPages[pageIndex] = new HelpPage(pageId, data);
                  this.pagesLoaded.update(n => n + 1);
                  if (this.pagesLoaded() === helpSet.pageIds.length) {
                     resolve(helpPages);
                  }
               },
               error: error => {
                  reject(error);
                  this.cancel();
               }
            }));
         });
      });
      this.fraction = computed(() => this.pagesLoaded() / helpSet.pageIds.length);
   }

   cancel(): void {
      this.httpSubscriptions.forEach(subscription => subscription.unsubscribe());
   }
}
