package lib.kalu.avselector.ui.selector;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import lib.kalu.avselector.R;
import lib.kalu.avselector.adapter.AlbumMediaAdapter;
import lib.kalu.avselector.loader.AlbumMediaCollection;
import lib.kalu.avselector.loader.SelectedItemCollection;
import lib.kalu.avselector.model.AlbumModel;
import lib.kalu.avselector.model.MediaModel;
import lib.kalu.avselector.model.SelectorModel;
import lib.kalu.avselector.util.LogUtil;
import lib.kalu.avselector.util.UIUtils;
import lib.kalu.avselector.widget.MediaGridInset;

public class SelectorFragment extends Fragment implements
        AlbumMediaCollection.AlbumMediaCallbacks, AlbumMediaAdapter.CheckStateListener,
        AlbumMediaAdapter.OnMediaClickListener {

    public static final String EXTRA_ALBUM = "extra_album";

    private final AlbumMediaCollection mAlbumMediaCollection = new AlbumMediaCollection();
    private RecyclerView mRecyclerView;
    private AlbumMediaAdapter mAdapter;
    private SelectionProvider mSelectionProvider;
    private AlbumMediaAdapter.CheckStateListener mCheckStateListener;
    private AlbumMediaAdapter.OnMediaClickListener mOnMediaClickListener;

    public static SelectorFragment newInstance(AlbumModel albumModel) {
        SelectorFragment fragment = new SelectorFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_ALBUM, albumModel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SelectionProvider) {
            mSelectionProvider = (SelectionProvider) context;
        } else {
            throw new IllegalStateException("Context must implement SelectionProvider.");
        }
        if (context instanceof AlbumMediaAdapter.CheckStateListener) {
            mCheckStateListener = (AlbumMediaAdapter.CheckStateListener) context;
        }
        if (context instanceof AlbumMediaAdapter.OnMediaClickListener) {
            mOnMediaClickListener = (AlbumMediaAdapter.OnMediaClickListener) context;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        LogUtil.logE("SelectorFragment => setUserVisibleHint => isVisibleToUser = "+isVisibleToUser);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        LogUtil.logE("SelectorFragment => onViewCreated =>");

        AlbumModel albumModel = (AlbumModel) getArguments().getSerializable(EXTRA_ALBUM);

        mAdapter = new AlbumMediaAdapter(getContext(),
                mSelectionProvider.provideSelectedItemCollection(), mRecyclerView);
        mAdapter.registerCheckStateListener(this);
        mAdapter.registerOnMediaClickListener(this);
        mRecyclerView.setHasFixedSize(true);

        int spanCount;
        SelectorModel selectorModel = SelectorModel.getInstance();
        if (selectorModel.gridExpectedSize > 0) {
            spanCount = UIUtils.spanCount(getContext(), selectorModel.gridExpectedSize);
        } else {
            spanCount = selectorModel.spanCount;
        }
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

        int spacing = getResources().getDimensionPixelSize(R.dimen.fs_d4);
        mRecyclerView.addItemDecoration(new MediaGridInset(spanCount, spacing, false));
        mRecyclerView.setAdapter(mAdapter);
        mAlbumMediaCollection.onCreate(getActivity(), this);
        mAlbumMediaCollection.load(albumModel, selectorModel.capture);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtil.logE("SelectorFragment => onDestroyView =>");
        mAlbumMediaCollection.onDestroy();
    }

    public void refreshMediaGrid() {
        mAdapter.notifyDataSetChanged();
    }

    public void refreshSelection() {
        mAdapter.refreshSelection();
    }

    @Override
    public void onAlbumMediaLoad(Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onAlbumMediaReset() {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onUpdate() {
        // notify outer Activity that check state changed
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public void onMediaClick(AlbumModel albumModel, MediaModel mediaModel, int adapterPosition) {
        if (mOnMediaClickListener != null) {
            mOnMediaClickListener.onMediaClick((AlbumModel) getArguments().getSerializable(EXTRA_ALBUM),
                    mediaModel, adapterPosition);
        }
    }

    public interface SelectionProvider {
        SelectedItemCollection provideSelectedItemCollection();
    }
}
