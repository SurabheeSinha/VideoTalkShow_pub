package com.example.videotalkshow;

import androidx.appcompat.app.AppCompatActivity;

import com.voxeet.promise.Promise;
import com.voxeet.sdk.services.chat.ChatMessageEvent;
import com.voxeet.sdk.json.BroadcastEvent;
import com.voxeet.sdk.models.v2.ChatMessage;
import com.voxeet.sdk.services.VoxeetDispatcher;
import com.voxeet.sdk.services.chat.ChatMessageEvent;
import com.voxeet.sdk.services.chat.ChatMessageEvent;

import android.media.MediaDrm;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.voxeet.VoxeetSDK;
import com.voxeet.android.media.MediaStream;
import com.voxeet.android.media.stream.MediaStreamType;
import com.voxeet.promise.solve.ErrorPromise;
import com.voxeet.promise.solve.ThenPromise;
import com.voxeet.sdk.events.v2.ParticipantAddedEvent;
import com.voxeet.sdk.events.v2.ParticipantUpdatedEvent;
import com.voxeet.sdk.events.v2.StreamAddedEvent;
import com.voxeet.sdk.events.v2.StreamRemovedEvent;
import com.voxeet.sdk.events.v2.StreamUpdatedEvent;
import com.voxeet.sdk.events.promises.ServerErrorException;
import com.voxeet.sdk.json.RecordingStatusUpdatedEvent;
import com.voxeet.sdk.json.ParticipantInfo;
import com.voxeet.sdk.json.internal.ParamsHolder;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.Participant;
import com.voxeet.sdk.models.v2.ChatMessageType;
import com.voxeet.sdk.services.builders.ConferenceCreateOptions;
import com.voxeet.sdk.services.builders.ConferenceJoinOptions;
import com.voxeet.sdk.services.conference.information.ConferenceInformation;
import com.voxeet.sdk.services.screenshare.RequestScreenSharePermissionEvent;
import com.voxeet.sdk.views.VideoView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.WebSocket;

public class MainActivity extends AppCompatActivity {

    protected List<View> views = new ArrayList<>();
    protected List<View> buttonsNotLoggedIn = new ArrayList<>();
    protected List<View> buttonsInConference = new ArrayList<>();
    protected List<View> buttonsNotInConference = new ArrayList<>();
    protected List<View> buttonsInOwnVideo = new ArrayList<>();
    protected List<View> buttonsNotInOwnVideo = new ArrayList<>();
    protected List<View> buttonInchat = new ArrayList<>();
    protected List<View> buttonsInOwnScreenShare = new ArrayList<>();
    protected List<View> buttonsNotInOwnScreenShare = new ArrayList<>();

    @Bind(R.id.user_name)
    EditText user_name;

    @Bind(R.id.role)
    EditText role;

    @Bind(R.id.conference_name)
    EditText conference_name;

    @Bind(R.id.video)
    protected VideoView video;

    @Bind(R.id.videoOther)
    protected VideoView videoOther;

    @Bind(R.id.participants)
    EditText participants;

    @Bind(R.id.chat_message)
    EditText chatMessage;

    @Bind(R.id.host_name)
    TextView host_name;

    @Bind(R.id.chatMessagerecieved)
    TextView chatMessageReceived;

    Date date = new Date();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //Initializing the SDK
        VoxeetSDK.initialize("XjmM5hWJg63y9E_KBflMJw==", "kD9gcG8KWIcvZ9XdzHrPSBcTM0Qet4rYa_u9ji_rN-8=");

        add(views, R.id.login);
        add(views, R.id.logout);

        add(buttonsNotLoggedIn, R.id.login);
        add(buttonsNotLoggedIn, R.id.user_name);

        add(buttonsInConference, R.id.logout);

        add(buttonsNotInConference, R.id.logout);


        // Set a random user name
        String[] allowedUsers = {
                "Host",
                "Viewer",
        };
        //Random r = new Random();
       //user_name.setText(allowedUsers[r.nextInt(allowedUsers.length)]);


        // Add the join button and enable it only when not in a conference
        add(views, R.id.join);
        add(buttonsNotInConference, R.id.join);

        // Set a default conference name
        conference_name.setText("Video Talk Show ");

        add(views, R.id.leave);
        add(buttonsInConference, R.id.leave);

        //adding the startVideo in the flow
        add(views, R.id.startVideo);
        add(buttonsInConference, R.id.startVideo);
        add(buttonsNotInOwnVideo, R.id.startVideo);

        //adding the stopVideo in the flow
        add(views, R.id.stopVideo);
        add(buttonsInConference, R.id.stopVideo);
        add(buttonsInOwnVideo, R.id.stopVideo);

        add(views, R.id.hostChat);
        add(buttonInchat, R.id.hostChat);
        add(buttonInchat, R.id.chat_message);

    }


    @Override
    protected void onResume() {
        super.onResume();
        //register the current activity in the SDK
        VoxeetSDK.instance().register(this);

        // permission check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 0x20);
        }

        //updated views to enable or disable the ones we want to
        updateViews();

    }
    private void updateViews() {

        //disable every view
        setEnabled(views, false);

        String role_permissions = role.getText().toString();

        //if the user is not connected, we will only enabled the not logged in buttons
        if (!VoxeetSDK.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }
        ConferenceInformation current = VoxeetSDK.conference().getCurrentConference();
        //we can now add the logic to manage our basic state
        if (null != current) {
                setEnabled(buttonsInConference, true);
           }
        else{
                setEnabled(buttonsNotInConference, true);
            }

        //Video enabled for host

        if(null!=current){
            if(role_permissions.equals("host")){

                    if (current.isOwnVideoStarted()) {
                        setEnabled(buttonsInOwnVideo, true);
                        setEnabled(buttonsNotInOwnVideo, false);
                        setEnabled(buttonInchat, true);
                        //participants.setVisibility(View.VISIBLE);
                    } else {
                        setEnabled(buttonsInOwnVideo, false);
                        setEnabled(buttonsNotInOwnVideo, true);
                        setEnabled(buttonInchat, true);
                        //participants.setVisibility(View.GONE);
                    }
            }
            else if (role_permissions.equals("viewer"))
                {
                    setEnabled(buttonsNotInOwnVideo, false);
                    setEnabled(buttonsInOwnVideo, false);
                    VoxeetSDK.conference().mute(false);
                }
        }

       /* if (null != current) {
            if (current.isOwnVideoStarted()) {
                setEnabled(buttonsInOwnVideo, true);
                setEnabled(buttonsNotInOwnVideo, false);
            } else {
                setEnabled(buttonsInOwnVideo, false);
                setEnabled(buttonsNotInOwnVideo, true);
            }
        }*/
    }

    private ErrorPromise error() {
        return error -> {
            Toast.makeText(MainActivity.this, "ERROR...", Toast.LENGTH_SHORT).show();
            error.printStackTrace();
            updateViews();
        };
    }

    private void setEnabled(List<View> views, boolean enabled) {
        for (View view : views) view.setEnabled(enabled);
    }

    private MainActivity add(List<View> list, int id) {
        list.add(findViewById(id));
        return this;
    }
    @Override
    protected void onPause() {
        //register the current activity in the SDK
        VoxeetSDK.instance().unregister(this);

        super.onPause();
    }

    @OnClick(R.id.login)
    public void onLogin() {
        VoxeetSDK.session().open(new ParticipantInfo(user_name.getText().toString(), "", ""))
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "log in successful", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());

    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSDK.session().close()
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                    updateViews();
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        ParamsHolder paramsHolder = new ParamsHolder();
        paramsHolder.setDolbyVoice(true);

        ConferenceCreateOptions conferenceCreateOptions = new ConferenceCreateOptions.Builder()
                .setConferenceAlias(conference_name.getText().toString())
                .setParamsHolder(paramsHolder)
                .build();

        VoxeetSDK.conference().create(conferenceCreateOptions)
                .then((ThenPromise<Conference, Conference>) conference -> {
                    ConferenceJoinOptions conferenceJoinOptions = new ConferenceJoinOptions.Builder(conference)
                            .build();


                    return VoxeetSDK.conference().join(conferenceJoinOptions);
                })
                .then(conference -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error((error_in) -> {
                    Toast.makeText(MainActivity.this, "Could not create conference", Toast.LENGTH_SHORT).show();
                });
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSDK.conference().leave()
                .then((result, solver) -> {
                    updateViews();
                    Toast.makeText(MainActivity.this, "left...", Toast.LENGTH_SHORT).show();
                }).error(error());
    }

    @OnClick(R.id.startVideo)
    public void onStartVideo() {

            VoxeetSDK.conference().startVideo()
                    .then((result, solver) -> updateViews())
                    .error(error());


    }

    private void updateStreams() {
        for (Participant user : VoxeetSDK.conference().getParticipants()) {
            boolean isLocal = user.getId().equals(VoxeetSDK.session().getParticipantId());
            MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.Camera);

            VideoView video = isLocal ? this.video : this.videoOther;

            if (null != stream && !stream.videoTracks().isEmpty()) {
                video.setVisibility(View.VISIBLE);
                video.attach(user.getId(), stream);
                host_name.setVisibility(View.VISIBLE);
                host_name.setText(user_name.getText().toString());
            }else{
                video.setVisibility(View.GONE);
                host_name.setVisibility(View.GONE);
            }

        }

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamAddedEvent event) {
        updateStreams();
        updateViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamUpdatedEvent event) {
        updateStreams();
        updateViews();
    }

    @OnClick(R.id.stopVideo)
    public void onStopVideo() {
        VoxeetSDK.conference().stopVideo()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    /*@OnClick(R.id.hostChat)
    public void Chat(){
        ChatMessage chatmessage = new ChatMessage(date, chatMessage.getText().toString(), ChatMessageType.TEXT );
        VoxeetSDK.chat().sendMessage(VoxeetSDK.conference().getConference(),chatmessage);
    }*/

  @OnClick(R.id.hostChat)
    public void send(){
      //String conferenceId = VoxeetSDK.conference().getConferenceId();
       VoxeetSDK.command().send(VoxeetSDK.conference().getConferenceId(), chatMessage.getText().toString()).then(Promise.resolve(0)).error((error_in) -> {
           Log.d("Error","Error message");
       });
      ChatMessage chatmessage = new ChatMessage(date, chatMessage.getText().toString(), ChatMessageType.TEXT );
      VoxeetSDK.chat().sendMessage(VoxeetSDK.conference().getConference(),chatmessage);
      Log.d( "Debug", chatMessage.getText().toString());
  }


    public void updateParticipants() {
        List<Participant> participantsList = VoxeetSDK.conference().getParticipants();
        List<String> names = new ArrayList<>();

        for (Participant participant : participantsList) {
            if (participant.streams().size() > 0)
                names.add(participant.getInfo().getName());
        }

        participants.setText(TextUtils.join(", ", names));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantAddedEvent event) {
        updateParticipants();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantUpdatedEvent event) {
        updateParticipants();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ChatMessageEvent event){
        String message = event.message.toString();
        String participant = event.participant.getId().toString();
        chatMessageReceived.setText(participant + ":"+message);
        Log.d( "Debug", participant + ":"+message);
  }


}