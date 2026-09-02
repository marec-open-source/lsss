import {Component, inject} from '@angular/core';
import {ConfigService} from './config.service';

@Component({
   selector: 'marec-footer',
   templateUrl: './footer.component.html',
   styleUrl: './footer.component.scss',
   imports: [],
})
export class FooterComponent {
   protected readonly year: number;

   constructor() {
      const configService = inject(ConfigService);
      this.year = new Date(configService.config.helpSets[0].buildTime).getFullYear();
   }
}
