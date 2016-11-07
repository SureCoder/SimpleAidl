package sure.simpleaidl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import myaidl.Book;
import myaidl.BookManager;
import myaidl.IOnNewBookArrivedListener;

/**
 * 描述：
 * 创建人：shuo
 * 创建时间：2016/11/7 22:36
 */
public class RemoteService extends Service {
    //远程Service,运行在另一个进程

    CopyOnWriteArrayList<Book> bookList = new CopyOnWriteArrayList<>();

    RemoteCallbackList<IOnNewBookArrivedListener> listeners = new RemoteCallbackList<>();

    @Override
    public void onCreate() {

        bookList.add(new Book("1",1));
        bookList.add(new Book("2",2));


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return bookManager;
    }

    BookManager.Stub bookManager = new BookManager.Stub() {
        //这些方法都运行在这个Service所在进程的子线程中（运行在Binder线程池中的线程）
        @Override
        public Book getBook(int index) throws RemoteException {
            return bookList.get(index);
        }

        @Override
        public void addBook(Book book) throws RemoteException {
                bookList.add(book);
        }

        @Override
        public void register(IOnNewBookArrivedListener listener) throws RemoteException {
                listeners.register(listener);
            //开启这个进程下的一个工作线程，当前在这个进程中有2个子线程，一个是这个方法运行的子线程，
            // 一个是下面开启的子线程，为什么要再开个子线程呢？ 因为虽然这个方法运行在子线程中，
            // 但是activity那边调用这个方法时会阻塞住，直到这个方法执行完成（其他几个方法也同理），
            // 所以在这里执行耗时操作会阻塞activity的主线程造成ANR，要执行耗时操作必须再开启个子线程
            new Thread(new Worker()).start();
        }

        @Override
        public void unregister(IOnNewBookArrivedListener listener) throws RemoteException {
                listeners.unregister(listener);
        }
    };

    class Worker implements Runnable{

        @Override
        public void run() {
            try {
                //8秒后天添加一本书
                Thread.sleep(3000);
                //通知书到了 RemoteCallbackList的用法
                int num = listeners.beginBroadcast();
                for (int i = 0; i < num; i++) {
                    IOnNewBookArrivedListener l= listeners.getBroadcastItem(i);
                    //当调用onNewBookArrived方法时，这个子线程会被阻塞住，直到这个远程方法执行完毕。
                    l.onNewBookArrived(new Book("100",100));
                }
                listeners.finishBroadcast();
                //通知完成
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
