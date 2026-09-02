import {Component, inject} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ConfigService} from './config.service';
import {ErrorResponseComponent} from './error-response.component';
import {ProgressSpinnerComponent} from './progress-spinner.component';

@Component({
   selector: 'marec-app',
   templateUrl: './app.component.html',
   styleUrl: './app.component.scss',
   imports: [
      RouterOutlet,
      ErrorResponseComponent, ProgressSpinnerComponent,
   ],
})
export class AppComponent {
   protected readonly configService: ConfigService = inject(ConfigService);
}
