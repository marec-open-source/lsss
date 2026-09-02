import {HttpErrorResponse} from '@angular/common/http';
import {Component, input, InputSignal} from '@angular/core';

@Component({
   selector: 'marec-error-response',
   templateUrl: './error-response.component.html',
   styleUrl: './error-response.component.scss',
   imports: [
   ],
})
export class ErrorResponseComponent {
   readonly error: InputSignal<HttpErrorResponse | undefined> = input();

   constructor() {
      // Intentionally empty.
   }
}
