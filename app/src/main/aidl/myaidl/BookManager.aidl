// BookManager.aidl
package myaidl;
import myaidl.Book;
import myaidl.IOnNewBookArrivedListener;
// Declare any non-default types here with import statements

interface BookManager {

    Book getBook(int index);
    void addBook(in Book book);
    void register(IOnNewBookArrivedListener listener);
    void unregister(IOnNewBookArrivedListener listener);

}
