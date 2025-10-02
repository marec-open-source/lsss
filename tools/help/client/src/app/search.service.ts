import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {inject, Injectable, signal, WritableSignal} from '@angular/core';
import {Index} from 'lunr';
import {Observable, of as observableOf, ReplaySubject} from 'rxjs';
import {map} from 'rxjs/operators';
import {ConfigService} from './config.service';
import {SearchResult} from './misc/SearchResult';

@Injectable({
   providedIn: 'root',
})
export class SearchService {
   private readonly configService: ConfigService = inject(ConfigService);
   private readonly http: HttpClient = inject(HttpClient);

   private inited: boolean = false;
   private loadingSubject?: ReplaySubject<void> = new ReplaySubject<void>();
   readonly loading: WritableSignal<boolean> = signal(true);
   readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);

   init(): void {
      if (this.inited) {
         return;
      }
      this.inited = true;
      this.http.get<object[]>(`api/lunrIndexes.json`).subscribe({
         next: lunrIndexes => {
            this.configService.config.helpSets.forEach((helpSet, i) => {
               helpSet.lunrIndex = Index.load(lunrIndexes[i]);
            });
            this.doneLoading();
         },
         error: error => {
            this.errorResponse.set(error);
            console.log(error);
            this.doneLoading();
         }
      });
   }

   private doneLoading(): void {
      this.loading.set(false);
      this.loadingSubject?.next();
      this.loadingSubject = undefined;
   }

   search(text: string): Observable<SearchResult[]> {
      this.init();
      if (this.loadingSubject) {
         return this.loadingSubject.pipe(map(() => this.doSearch(text)));
      }
      if (this.errorResponse()) {
         return observableOf([]);
      }
      return observableOf(this.doSearch(text));
   }

   private doSearch(text: string): SearchResult[] {
      if (!text) {
         return [];
      }
      const searchResults: SearchResult[] = [];
      this.configService.config.helpSets.forEach(helpSet => {
         if (!helpSet.lunrIndex) {
            console.log('Missing lunrIndex');
            return;
         }
         helpSet.lunrIndex.search(text).forEach(result => {
            const pageIndex = parseInt(result.ref, 36);
            const pageId = helpSet.pageIds[pageIndex];
            const title = helpSet.pageTitles[pageIndex];
            const tocItem = helpSet.pageIdToTocItem[pageId];
            searchResults.push(new SearchResult(result.score, title, tocItem));
         });
      });
      searchResults.sort((a, b) => b.score - a.score || a.title.localeCompare(b.title));
      return searchResults;
   }
}
