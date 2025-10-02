import * as Utils from './Utils';

describe('Utils', () => {
   // beforeEach(() => {
   // });

   it('should normalize url', () => {
      expect(Utils.normalizeUrl('a/b/c', 'x/y')).toEqual('a/b/c/x/y');
      expect(Utils.normalizeUrl('a/b/c', './x/y')).toEqual('a/b/c/x/y');
      expect(Utils.normalizeUrl('a/b/c', '././x/y')).toEqual('a/b/c/x/y');
      expect(Utils.normalizeUrl('a/b/c', '../x/y')).toEqual('a/b/x/y');
      expect(Utils.normalizeUrl('a/b/c', '../../x/y')).toEqual('a/x/y');
   });
});
