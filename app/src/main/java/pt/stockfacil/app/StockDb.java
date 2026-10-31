package pt.stockfacil.app;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StockDb extends SQLiteOpenHelper {
    public StockDb(Context context) { super(context, "stockfacil.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (barcode TEXT PRIMARY KEY, name TEXT NOT NULL, qty INTEGER NOT NULL DEFAULT 0 CHECK(qty >= 0), min_qty INTEGER NOT NULL DEFAULT 5)");
        db.execSQL("CREATE TABLE movements (id INTEGER PRIMARY KEY AUTOINCREMENT, barcode TEXT NOT NULL, product_name TEXT NOT NULL, delta INTEGER NOT NULL, date TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    public static class Product {
        public final String barcode, name; public final int qty, min;
        Product(String b, String n, int q, int m) { barcode=b; name=n; qty=q; min=m; }
    }
    public Product get(String barcode) {
        try (Cursor c=getReadableDatabase().rawQuery("SELECT barcode,name,qty,min_qty FROM products WHERE barcode=?", new String[]{barcode})) {
            return c.moveToFirst()? new Product(c.getString(0),c.getString(1),c.getInt(2),c.getInt(3)):null;
        }
    }
    public void saveProduct(String barcode, String name, int min) {
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues(); v.put("barcode",barcode); v.put("name",name); v.put("min_qty",min);
        db.insertWithOnConflict("products",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        db.update("products",v,"barcode=?",new String[]{barcode});
    }
    public boolean move(String barcode, int delta) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            Product p=get(barcode);
            if(p==null || p.qty+delta<0) return false;
            db.execSQL("UPDATE products SET qty=qty+? WHERE barcode=?",new Object[]{delta,barcode});
            ContentValues v=new ContentValues(); v.put("barcode",barcode); v.put("product_name",p.name); v.put("delta",delta);
            v.put("date",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date()));
            db.insertOrThrow("movements",null,v); db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
    public ArrayList<Product> products() {
        ArrayList<Product> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT barcode,name,qty,min_qty FROM products ORDER BY name COLLATE NOCASE",null)) {
            while(c.moveToNext()) result.add(new Product(c.getString(0),c.getString(1),c.getInt(2),c.getInt(3)));
        } return result;
    }
    public ArrayList<String> history() {
        ArrayList<String> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT date,product_name,barcode,delta FROM movements ORDER BY id DESC LIMIT 200",null)) {
            while(c.moveToNext()) result.add(c.getString(0)+"\n"+c.getString(1)+" ("+c.getString(2)+")  "+(c.getInt(3)>0?"+":"")+c.getInt(3));
        } return result;
    }
}
