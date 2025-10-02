import {ChangeDetectionStrategy, Component, inject, OnDestroy} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs';
import {ConfigService} from './config.service';
import {NavItem} from './misc/NavItem';

@Component({
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-navigation',
   template: '',
   styles: '',
   imports: [],
})
export class NavigationComponent implements OnDestroy {
   private routeSubscription: Subscription;

   constructor() {
      const configService = inject(ConfigService);
      const activatedRoute = inject(ActivatedRoute);

      configService.navigation.next(undefined);

      this.routeSubscription = activatedRoute.paramMap.subscribe(paramMap => {

         const helpSetId = paramMap.get('helpSet') ?? '';
         const pageId = paramMap.get('page') ?? '';
         const anchor = paramMap.get('anchor') ?? '';

         const helpSet = configService.idToHelpSet(helpSetId);
         if (!helpSet) {
            const firstHelpSet = configService.config.helpSets[0];
            configService.replaceNavigationToTop(firstHelpSet);
            return;
         }

         const alias = helpSet.aliases[pageId];
         if (alias) {
            configService.replaceNavigation(new NavItem(helpSet, helpSet.pageIds[alias[0]], alias[1] ?? pageId));
            return;
         }

         const pageTocItem = helpSet.pageIdToTocItem[pageId];
         if (!pageTocItem) {
            if (configService.routePrefix === '/set' && !pageId && !anchor) {
               configService.navigation.next(new NavItem(helpSet));
               return;
            }
            configService.replaceNavigationToTop(helpSet);
            return;
         }

         configService.navigation.next(new NavItem(helpSet, pageId, anchor));
      });
   }

   ngOnDestroy(): void {
      this.routeSubscription.unsubscribe();
   }
}
