package sure.simpleaidl;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import myaidl.Book;
import myaidl.BookManager;
import myaidl.IOnNewBookArrivedListener;

public class MainActivity extends AppCompatActivity {

    TextView tv;
    BookManager bookManager;

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            bookManager = BookManager.Stub.asInterface(iBinder);
            //设置死亡代理
            try {
                iBinder.linkToDeath(deathRecipient,0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
    }

    public void click(View v) {
        try {
            Book book;
            switch (v.getId()) {
                case R.id.bind:
                    Intent intent = new Intent(this, RemoteService.class);
                    bindService(intent, connection, BIND_AUTO_CREATE);
                    break;
                case R.id.getBook:
                    //调用此远程方法（另一个进程的方法）会挂起当前线程(在这里是主线程)，
                    //所以如果getBook比较耗时，那么需要开个子线程来调用
                    book = bookManager.getBook(0);
                    tv.setText(book.toString());
                    break;
                case R.id.addBook:
                    //这里传过去的Book和那个进程接收到的Book对象只是数据相同，并不是同一个内存地址的Book对象
                    bookManager.addBook(new Book("3", 3));
                    book = bookManager.getBook(2);
                    tv.setText(book.toString());
                    break;
                case R.id.notify:
                    bookManager.register(listener);

                    break;


            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //Binder死亡代理
    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (bookManager == null){
                return;
            }
            bookManager.asBinder().unlinkToDeath(deathRecipient,0);
            bookManager = null;
            //死亡后重新绑定service
            Intent intent = new Intent(MainActivity.this, RemoteService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }
    };

    //远程listenner的实现
    private IOnNewBookArrivedListener.Stub listener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            //这个方法运行在这个Activity所在进程的子线程（运行在Binder线程池中的线程）
            //所以要用handler操作UI
            Message message = handler.obtainMessage(0, book);
            handler.sendMessage(message);
        }
    };

    //发消息到主线程
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Book book = (Book) msg.obj;
            tv.setText(book.toString());
        }
    };

    @Override
    protected void onDestroy() {
        //判断并取消注册以及注销远程服务
        if (bookManager != null && bookManager.asBinder().isBinderAlive()) {
            try {
                bookManager.unregister(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(connection);
        super.onDestroy();
    }
}
