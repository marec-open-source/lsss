import {TocItem} from '../api/TocItem';

export class SearchResult {
   constructor(public score: number,
               public title: string,
               public tocItem: TocItem) {
   }
}
