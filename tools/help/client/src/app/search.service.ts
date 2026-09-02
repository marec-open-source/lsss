import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {inject, Service, signal, WritableSignal} from '@angular/core';
import {Index} from 'lunr';
import {ConfigService} from './config.service';
import {SearchResult} from './misc/SearchResult';

@Service()
export class SearchService {
   private readonly configService: ConfigService = inject(ConfigService);
   private readonly http: HttpClient = inject(HttpClient);

   private inited: boolean = false;
   readonly loading: WritableSignal<boolean> = signal(true);
   readonly searchFunction: WritableSignal<(text: string) => SearchResult[]> = signal(() => []);
   readonly errorResponse: WritableSignal<HttpErrorResponse | undefined> = signal(undefined);

   init(): void {
      if (this.inited) {
         return;
      }
      this.inited = true;
      this.http.get<object[]>('api/lunrIndexes.json').subscribe({
         next: lunrIndexes => {
            this.configService.config.helpSets.forEach((helpSet, i) => {
               helpSet.lunrIndex = Index.load(lunrIndexes[i]);
            });
            this.searchFunction.set(text => this.doSearch(text));
            this.loading.set(false);
         },
         error: error => {
            console.error(error);
            this.errorResponse.set(error);
            this.loading.set(false);
         }
      });
   }

   private doSearch(text: string): SearchResult[] {
      if (!text) {
         return [];
      }
      const searchResults: SearchResult[] = [];
      this.configService.config.helpSets.forEach(helpSet => {
         if (!helpSet.lunrIndex) {
            console.error('Missing lunrIndex');
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
