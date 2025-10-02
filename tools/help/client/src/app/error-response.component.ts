import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, input, InputSignal} from '@angular/core';

@Component({
   changeDetection: ChangeDetectionStrategy.OnPush,
   selector: 'marec-error-response',
   templateUrl: './error-response.component.html',
   styleUrl: './error-response.component.css',
   imports: [
   ],
})
export class ErrorResponseComponent {
   readonly error: InputSignal<HttpErrorResponse | undefined> = input();

   constructor() {
      // Intentionally empty.
   }
}
