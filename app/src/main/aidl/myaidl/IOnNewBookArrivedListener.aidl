// IOnNewBookArrivedListener.aidl
package myaidl;
import myaidl.Book;
// Declare any non-default types here with import statements

interface IOnNewBookArrivedListener {

      void onNewBookArrived(in Book book);
}
