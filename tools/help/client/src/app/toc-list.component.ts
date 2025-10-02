import {ChangeDetectionStrategy, Component, inject, input, InputSignal} from '@angular/core';
import {TocItem} from './api/TocItem';
import {ConfigService} from './config.service';

@Component({
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-toc-list',
   templateUrl: './toc-list.component.html',
   styleUrl: './toc-list.component.css',
   imports: [
   ],
})
export class TocListComponent {
   private readonly configService: ConfigService = inject(ConfigService);

   readonly tocItems: InputSignal<TocItem[] | undefined> = input();

   tocLink(tocItem: TocItem): string {
      return this.configService.navItemToLink(tocItem.navItem);
   }
}
