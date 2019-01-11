package in.magictel.transferdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import in.magictel.transferdemo.transfer.TransferManager;
import in.magictel.transferdemo.transfer.TransferService;
import in.magictel.transferdemo.transfer.TransferStatus;
import in.magictel.transferdemo.util.Permissions;
import in.magictel.transferdemo.util.Settings;

public class TransferActivity extends AppCompatActivity {

    private final String TAG = TransferActivity.this.getLocalClassName();
    private Settings mSettings;

    private ImageView mIcon;
    private TextView mDevice;
    private TextView mState;
    private ProgressBar mProgress;
    private TextView mBytes;
    private TintableButton mStop;
    private SparseArray<TransferStatus> mStatuses = new SparseArray<>();


    private BroadcastReceiver mBroadcastReceiver;
    TextView mTextView;


    /**
     * Finish initializing the activity
     */
    private void finishInit() {
        Log.i(TAG, "finishing initialization of activity");

        mIcon = findViewById(R.id.transfer_icon);
        mDevice = findViewById(R.id.transfer_device);
        mState = findViewById(R.id.transfer_state);
        mProgress = findViewById(R.id.transfer_progress);
        mBytes = findViewById(R.id.transfer_bytes);
        mStop = findViewById(R.id.transfer_action);

        // This never changes
        mStop.setIcon(R.drawable.ic_action_stop);
        mStop.setText(R.string.adapter_transfer_stop);



        // Setup the empty view
        // Create layout parameters for full expansion
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        // Create a container
        ViewGroup parentView = new LinearLayout(this);
        parentView.setLayoutParams(layoutParams);
        mTextView = new TextView(this);
        mTextView.setGravity(Gravity.CENTER);
        mTextView.setLayoutParams(layoutParams);
        mTextView.setTextColor(mTextView.getTextColors().withAlpha(60));
        mTextView.setText(R.string.activity_transfer_empty_text);
        parentView.addView(mTextView);

        // Launch the transfer service if it isn't already running
        TransferService.startStopService(this, mSettings.getBoolean(Settings.Key.BEHAVIOR_RECEIVE));
    }

    /**
     * Update the information for a transfer in the sparse array
     */
    void update(TransferStatus transferStatus) {
        int index = mStatuses.indexOfKey(transferStatus.getId());
        if (index < 0) {
            mStatuses.put(transferStatus.getId(), transferStatus);
        } else {
            mStatuses.setValueAt(index, transferStatus);
            bindViews(index,TransferActivity.this);
        }
    }

    /**
     * Retrieve the status for the specified index
     */
    TransferStatus getStatus(int index) {
        return mStatuses.valueAt(index);
    }

    /**
     * Remove the specified transfer from the sparse array
     */
    void remove(int index) {
        mStatuses.removeAt(index);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Settings(this);

        setContentView(R.layout.activity_transfer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup the floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // startActivity(new Intent(TransferActivity.this, ExplorerActivity.class));
            }
        });

        if (!Permissions.haveStoragePermission(this)) {
            Permissions.requestStoragePermission(this);
        } else {
            finishInit();
        }

        // Setup the broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TransferStatus transferStatus = intent.getParcelableExtra(TransferManager.EXTRA_STATUS);
                update(transferStatus);

               /* if (*//*adapter.getItemCount() == 1*//*) {*/
                    mTextView.setVisibility(View.GONE);
               /* }*/
            }
        };
    }

    /**
     * Method to update view
     * @param position
     * @param mContext
     */
    public void  bindViews(int position/*pass it 0 from testing*/, final Context mContext){
        final TransferStatus transferStatus = mStatuses.valueAt(position);

        // Generate transfer byte string
        CharSequence bytesText;
        if (transferStatus.getBytesTotal() == 0) {
            bytesText = getString(R.string.adapter_transfer_unknown);
        } else {
            bytesText = getString(
                    R.string.adapter_transfer_bytes,
                    Formatter.formatShortFileSize(this, transferStatus.getBytesTransferred()),
                    Formatter.formatShortFileSize(this, transferStatus.getBytesTotal())
            );
        }

        // Set the attributes
        mIcon.setImageResource(R.drawable.stat_download);
        mDevice.setText(transferStatus.getRemoteDeviceName());
        mProgress.setProgress(transferStatus.getProgress());
        mBytes.setText(bytesText);

        // Display the correct state string in the correct style
        switch (transferStatus.getState()) {
            case Connecting:
            case Transferring:
                if (transferStatus.getState() == TransferStatus.State.Connecting) {
                   mState.setText(R.string.adapter_transfer_connecting);
                } else {
                    mState.setText(mContext.getString(R.string.adapter_transfer_transferring,
                            transferStatus.getProgress()));
                }
                mState.setTextColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                mStop.setVisibility(View.VISIBLE);
                mStop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent stopIntent = new Intent(mContext, TransferService.class)
                                .setAction(TransferService.ACTION_STOP_TRANSFER)
                                .putExtra(TransferService.EXTRA_TRANSFER, transferStatus.getId());
                        mContext.startService(stopIntent);
                    }
                });
                break;
            case Succeeded:
                mState.setText(R.string.adapter_transfer_succeeded);
               /* mState.setTextColor(ContextCompat.getColor(mContext,
                        mSettings.getTheme() == R.style.LightTheme ?
                                R.color.colorSuccess : R.color.colorSuccessDark));*/
                mStop.setVisibility(View.INVISIBLE);
                break;
            case Failed:
                mState.setText(mContext.getString(R.string.adapter_transfer_failed,
                        transferStatus.getError()));
              /*  mState.setTextColor(ContextCompat.getColor(mContext,
                        mSettings.getTheme() == R.style.LightTheme ?
                                R.color.colorError : R.color.colorErrorDark));*/
                mStop.setVisibility(View.INVISIBLE);
                break;
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        Log.i(TAG, "registering broadcast receiver");

        // Start listening for broadcasts
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(TransferManager.TRANSFER_UPDATED));

        // Get fresh data from the service
        Intent broadcastIntent = new Intent(TransferActivity.this, TransferService.class)
                .setAction(TransferService.ACTION_BROADCAST);
       startService(broadcastIntent);
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.i(TAG, "unregistering broadcast receiver");

        // Stop listening for broadcasts
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (Permissions.obtainedStoragePermission(requestCode, grantResults)) {
            finishInit();
        } else {
            finish();
        }
    }
}
