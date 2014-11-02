package dk.simplecontentprovider;

import android.content.UriMatcher;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class SimpleUriMatcher {
    private UriMatcher mUriMatcher;
    private List<Match> mMatches;

    public SimpleUriMatcher(String authority, List<SimpleContentProvider.Entity> entities, List<SimpleContentProvider.EntityView> views) {
        int numberOfMatches = 2 * (entities.size() + views.size());

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatches = new ArrayList<Match>(numberOfMatches);
        int location = 0;

        for (SimpleContentProvider.Entity entity : entities) {
            mUriMatcher.addURI(authority, entity.name, location);
            mMatches.add(new Match(entity, false));
            location += 1;

            mUriMatcher.addURI(authority, entity.name + "/#", location);
            mMatches.add(new Match(entity, true));
            location += 1;
        }

        for (SimpleContentProvider.EntityView view : views) {
            mUriMatcher.addURI(authority, view.name, location);
            mMatches.add(new Match(view, false));
            location += 1;

            mUriMatcher.addURI(authority, view.name + "/#", location);
            mMatches.add(new Match(view, true));
            location += 1;
        }
    }

    public Match match(Uri uri) {
        int location = mUriMatcher.match(uri);
        if (location != -1) {
            return mMatches.get(location);
        }

        return null;
    }

    protected static class Match {
        protected final SimpleContentProvider.Entity entity;
        protected final SimpleContentProvider.EntityView view;
        protected final boolean isItem;

        public Match(SimpleContentProvider.Entity entity, boolean isItem) {
            this.entity = entity;
            this.view = null;
            this.isItem = isItem;
        }

        public Match(SimpleContentProvider.EntityView view, boolean isItem) {
            this.entity = null;
            this.view = view;
            this.isItem = isItem;
        }

        public boolean isItem() {
            return isItem;
        }
    }
}
