package com.fusionx.lightirc.ui;

import com.fusionx.bus.Subscribe;
import com.fusionx.lightirc.R;
import com.fusionx.lightirc.event.OnConversationChanged;
import com.fusionx.lightirc.ui.dialogbuilder.DialogBuilder;
import com.fusionx.lightirc.ui.dialogbuilder.NickDialogBuilder;
import com.fusionx.lightirc.util.FragmentUtils;

import org.lucasr.twowayview.TwoWayView;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.fusionx.relay.conversation.Conversation;
import co.fusionx.relay.core.Session;

import static com.fusionx.lightirc.util.MiscUtils.getBus;

public class ActionsFragment extends Fragment {

    private Conversation mConversation;

    private Session mConnection;

    private final Object mEventHandler = new Object() {
        @Subscribe
        public void onEvent(final OnConversationChanged conversationChanged) {
            mConnection = conversationChanged.session;
            mConversation = conversationChanged.conversation;
        }
    };

    private Callbacks mCallbacks;

    private ActionsAdapter mAdapter;

    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;

    private TwoWayView mTwoWayView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallbacks = FragmentUtils.getParent(this, Callbacks.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ActionsAdapter(getActivity(), new ActionClickListner());
        mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getActivity(),
                R.layout.sliding_menu_header, R.id.sliding_menu_heading_textview, mAdapter);
        mAdapter.setSectionedAdapter(mSectionedAdapter);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBus().registerSticky(mEventHandler);

        mTwoWayView = (TwoWayView) view.findViewById(android.R.id.list);
        mTwoWayView.setAdapter(mSectionedAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getBus().unregister(mEventHandler);
    }

    private void showNickDialog() {
        final NickDialogBuilder nickDialog = new ChannelNickDialogBuilder();
        nickDialog.show();
    }

    private void showChannelDialog() {
        final ChannelDialogBuilder builder = new ChannelDialogBuilder();
        builder.show();
    }

    private void showIgnoreUserFragment() {
        final IgnoredUsersFragment fragment = IgnoredUsersFragment.createInstance();
        fragment.show(getFragmentManager(), "ignoreFragment");
    }

    private void showInviteFragment() {
        final InviteFragment fragment = InviteFragment.createInstance();
        fragment.show(getFragmentManager(), "inviteFragment");
    }

    private void showPendingDCCFragment() {
        final DCCPendingFragment fragment = DCCPendingFragment.createInstance();
        fragment.show(getFragmentManager(), "dialog");
    }

    public interface Callbacks {

        public void removeCurrentFragment();

        public void closeDrawer();

        public void disconnectFromServer();

        public void reconnectToServer();
    }

    public class ChannelDialogBuilder extends DialogBuilder {

        public ChannelDialogBuilder() {
            super(getActivity(), getString(R.string.prompt_dialog_channel_name),
                    getString(R.string.prompt_dialog_including_starting), "");
        }

        @Override
        public void onOkClicked(final String channelName) {
            // If the conversation is null (for some reason or another) then simply close the dialog
            if (mConversation == null) {
                return;
            }
            mConnection.getServer().sendJoin(channelName);
        }
    }

    private class ActionClickListner implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            final int position = mTwoWayView.getChildPosition(v);
            final int actual = mSectionedAdapter.sectionedPositionToPosition(position);

            final String action = mAdapter.getItem(actual);
            if (action.equals(getString(R.string.action_join_channel))) {
                showChannelDialog();
            } else if (action.equals(getString(R.string.action_change_nick))) {
                showNickDialog();
            } else if (action.equals(getString(R.string.action_ignore_list))) {
                showIgnoreUserFragment();
            } else if (action.equals(getString(R.string.action_pending_dcc))) {
                showPendingDCCFragment();
            } else if (action.equals(getString(R.string.action_pending_invites))) {
                showInviteFragment();
            } else if (action.equals(getString(R.string.action_disconnect))) {
                mCallbacks.disconnectFromServer();
            } else if (action.equals(getString(R.string.action_close_server))) {
                mCallbacks.disconnectFromServer();
            } else if (action.equals(getString(R.string.action_reconnect))) {
                mCallbacks.reconnectToServer();
            } else if (action.equals(getString(R.string.action_part_channel))) {
                mCallbacks.removeCurrentFragment();
            } else if (action.equals(getString(R.string.action_close_pm))) {
                mCallbacks.removeCurrentFragment();
            }
            mCallbacks.closeDrawer();
        }
    }

    private class ChannelNickDialogBuilder extends NickDialogBuilder {

        public ChannelNickDialogBuilder() {
            super(getActivity(), mConnection.getUserChannelManager().getUser()
                    .getNick().getNickAsString());
        }

        @Override
        public void onOkClicked(final String nick) {
            // If the conversation is null (for some reason) then simply close the dialog
            if (mConnection == null || mConversation == null) {
                return;
            }
            mConnection.getServer().sendNick(nick);
        }
    }
}