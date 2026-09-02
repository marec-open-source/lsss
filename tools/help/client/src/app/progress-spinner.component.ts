import {Component, effect, input, InputSignal, signal, WritableSignal} from '@angular/core';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

@Component({
   selector: 'marec-progress-spinner',
   templateUrl: './progress-spinner.component.html',
   styleUrl: './progress-spinner.component.scss',
   imports: [
      MatProgressSpinnerModule,
   ],
})
export class ProgressSpinnerComponent {
   readonly show: InputSignal<boolean> = input(false);
   readonly determinate: InputSignal<boolean> = input(false);
   readonly fraction: InputSignal<number> = input(0);
   protected readonly visible: WritableSignal<boolean> = signal(false);

   constructor() {
      effect(() => {
         if (this.show()) {
            setTimeout(() => this.visible.set(this.show()), 100);
         } else {
            this.visible.set(false);
         }
      });
   }
}
