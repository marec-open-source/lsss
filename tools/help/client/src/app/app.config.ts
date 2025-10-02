import {provideHttpClient} from '@angular/common/http';
import {ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection} from '@angular/core';
import {provideRouter, withHashLocation} from '@angular/router';
import {routes} from './app.routes';

export const appConfig: ApplicationConfig = {
   providers: [
      provideBrowserGlobalErrorListeners(),
      provideHttpClient(),
      provideZonelessChangeDetection(),
      provideRouter(routes, withHashLocation()),
   ]
};
