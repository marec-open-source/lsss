import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {ConfigService} from './config.service';

@Component({
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-menu',
   templateUrl: './menu.component.html',
   styleUrl: './menu.component.scss',
   imports: [
      MatButtonModule, MatMenuModule,
   ],
})
export class MenuComponent {
   protected readonly configService: ConfigService = inject(ConfigService);
}
