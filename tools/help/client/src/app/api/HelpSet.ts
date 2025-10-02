import {Index} from 'lunr';
import {TocItem} from './TocItem';

export interface HelpSet {
   id: string;
   version: string;
   buildTime: string;
   name: string;
   icon: string;
   path: string;
   pageIds: string[];
   pageHrefs: string[];
   pageTitles: string[];
   aliases: Record<string, [number] | [number, string]>; // alias -> [ pageIndex, anchor? ]
   toc: TocItem[];
   //
   // From separate request:
   lunrIndex?: Index;
   //
   // Derived:
   imgSrc: string;
   pageIdToTocItem: Record<string, TocItem>;
   hrefToTocItem: Record<string, TocItem>;
}
