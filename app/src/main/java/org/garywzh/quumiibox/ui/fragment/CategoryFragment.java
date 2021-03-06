package org.garywzh.quumiibox.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.common.eventbus.Subscribe;
import com.umeng.analytics.MobclickAgent;

import org.garywzh.quumiibox.AppContext;
import org.garywzh.quumiibox.R;
import org.garywzh.quumiibox.common.UserState;
import org.garywzh.quumiibox.eventbus.CatagoryChangedEvent;
import org.garywzh.quumiibox.ui.MainActivity;
import org.garywzh.quumiibox.ui.adapter.CategoryAdapter;
import org.garywzh.quumiibox.util.ExecutorUtils;

import java.util.List;

public class CategoryFragment extends Fragment implements CategoryAdapter.OnStartActionModeListener {
    private static final String TAG = CategoryFragment.class.getSimpleName();

    private CategoryAdapter.OnCateItemClickListener mOnItemClickListener;
    private CategoryAdapter mCategoryAdapter;

    public CategoryFragment() {
        // Required empty public constructor
    }

    public static CategoryFragment newInstance() {
        return new CategoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_category, container, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        mCategoryAdapter = new CategoryAdapter(mOnItemClickListener, this);
        recyclerView.setAdapter(mCategoryAdapter);

        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        AppContext.getEventBus().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        AppContext.getEventBus().unregister(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final MainActivity activity = ((MainActivity) getActivity());
        activity.setTitle(getString(R.string.drawer_tag));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnItemClickListener = (CategoryAdapter.OnCateItemClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnItemActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnItemClickListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_cateitem_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            showAddItemDialog();
            return true;
        } else if (id == R.id.action_reset) {
            showResetDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStartActionMode(final MultiSelector multiSelector) {
        ((AppCompatActivity) getActivity()).startSupportActionMode(new ModalMultiSelectorCallback(multiSelector) {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                super.onCreateActionMode(actionMode, menu);
                getActivity().getMenuInflater().inflate(R.menu.menu_action_mode, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_delete) {
                    actionMode.finish();

                    List<String> items = UserState.getInstance().getCategoryItems();
                    for (int i = items.size() - 1; i >= 0; i--) {
                        if (multiSelector.isSelected(i, 0)) {
                            items.remove(items.get(i));
                        }
                    }
                    mCategoryAdapter.notifyDataSetChanged();
                    UserState.getInstance().updateCategoryItemsPrefs();
                    multiSelector.clearSelections();
                    return true;
                }
                return false;
            }
        });
    }

    private void showAddItemDialog() {
        AddItemAlertDialogFragment dialogFragment = AddItemAlertDialogFragment.newInstance();
        dialogFragment.show(getFragmentManager(), "additemdialog");
    }

    private void showResetDialog() {
        ResetAlertDialogFragment dialogFragment = ResetAlertDialogFragment.newInstance();
        dialogFragment.show(getFragmentManager(), "resetdialog");
    }

    @Subscribe
    public void onCatagoryChangedEvent(CatagoryChangedEvent e) {
        mCategoryAdapter.notifyDataSetChanged();
    }

    public static class ResetAlertDialogFragment extends DialogFragment {
        public static ResetAlertDialogFragment newInstance() {
            return new ResetAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            OnResetDialogClickListener onResetDialogClickListener = new OnResetDialogClickListener();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("将丢失修改的自定义内容")
                    .setTitle("重置")
                    .setPositiveButton(R.string.alert_dialog_ok, onResetDialogClickListener)
                    .setNegativeButton(R.string.alert_dialog_cancel, onResetDialogClickListener)
                    .create();
        }

        static class OnResetDialogClickListener implements DialogInterface.OnClickListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_NEGATIVE:
                        break;
                    case Dialog.BUTTON_POSITIVE:
                        ExecutorUtils.execute(new Runnable() {
                            @Override
                            public void run() {
                                UserState.getInstance().resetCateItemPrefs();
                                AppContext.getEventBus().post(new CatagoryChangedEvent());
                            }
                        });
                        break;
                }
            }
        }
    }

    public static class AddItemAlertDialogFragment extends DialogFragment {

        public static AddItemAlertDialogFragment newInstance() {
            return new AddItemAlertDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            int margin_in_dp = 24;
            final float scale = getResources().getDisplayMetrics().density;
            int margin_in_px = (int) (margin_in_dp * scale + 0.5f);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = margin_in_px;
            params.rightMargin = margin_in_px;
            final EditText input = new EditText(activity);
            input.setSingleLine(true);
            input.setLayoutParams(params);
            FrameLayout container = new FrameLayout(activity);
            container.addView(input);
            OnAddItemDialogClickListener onDialogClickListener = new OnAddItemDialogClickListener(input);

            return new AlertDialog.Builder(getActivity())
                    .setTitle("添加一个标签")
                    .setView(container)
                    .setPositiveButton(R.string.alert_dialog_ok, onDialogClickListener)
                    .setNegativeButton(R.string.alert_dialog_cancel, onDialogClickListener)
                    .create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        static class OnAddItemDialogClickListener implements DialogInterface.OnClickListener {
            private EditText mEditText;

            public OnAddItemDialogClickListener(EditText editText) {
                super();
                mEditText = editText;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_NEGATIVE:
                        break;
                    case Dialog.BUTTON_POSITIVE:
                        if (mEditText.getText().toString().trim().length() == 0) {
                            return;
                        }
                        ExecutorUtils.execute(new Runnable() {
                            @Override
                            public void run() {
                                UserState.getInstance().addCateoryItem(mEditText.getText().toString());
                                AppContext.getEventBus().post(new CatagoryChangedEvent());
                            }
                        });
                        break;
                }
            }
        }
    }
}