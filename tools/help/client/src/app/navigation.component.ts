import {Component, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute} from '@angular/router';
import {ConfigService} from './config.service';
import {NavItem} from './misc/NavItem';

@Component({
   selector: 'marec-navigation',
   template: '',
   styles: '',
   imports: [],
})
export class NavigationComponent {
   constructor() {
      const configService = inject(ConfigService);
      const activatedRoute = inject(ActivatedRoute);

      configService.navigation.set(undefined);

      activatedRoute.paramMap.pipe(takeUntilDestroyed()).subscribe(paramMap => {

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
               configService.navigation.set(new NavItem(helpSet));
               return;
            }
            configService.replaceNavigationToTop(helpSet);
            return;
         }

         configService.navigation.set(new NavItem(helpSet, pageId, anchor));
      });
   }
}
