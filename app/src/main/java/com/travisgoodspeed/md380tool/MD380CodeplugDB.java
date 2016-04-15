package com.travisgoodspeed.md380tool;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by travis on 4/13/16.
 *
 * This class converts a Codeplug into a SQLite database.  It is not yet functional, but in the
 * near future it will allow for codeplugs to be edited on the phone and then re-exported
 * to the device.  Unlike the MD380Codeplug class, it obliterates most of the understood portion
 * of the codeplug.
 */
public class MD380CodeplugDB {
    MD380Codeplug codeplug=null;
    SQLiteDatabase db=null;
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE CONTACTS(id, llid, flag, name);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS CONTACTS";

    public class CodeplugDbHelper extends SQLiteOpenHelper {
        //If you change the schema, increment the database version.
        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "md380codeplug.db";


        public CodeplugDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }

    Context context=null;
    public MD380CodeplugDB(Context context){
        this.context=context;
        CodeplugDbHelper helper=new CodeplugDbHelper(context);
        db=helper.getWritableDatabase();
    }

    /* This imports a codeplug file into the SQLite database. */
    public void importCodeplug(MD380Codeplug codeplug) {
        //First we grab the codeplug object.
        this.codeplug=codeplug;

        Log.d("CodeplugDB", "Dropping and recreating the old tables for the newly imported codeplug.");
        //Wipe the old tables and begin new ones.
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);

        //Populate the tables.
        Log.d("CodeplugDB", "Inserting Contacts");
        for(int i=1;i<=1000;i++){
            MD380Contact c=codeplug.getContact(i);
            if(c!=null){
                ContentValues values=new ContentValues(3);
                values.put("id",c.id);
                values.put("llid",c.llid);
                values.put("flag",c.flags);
                values.put("name",c.nom);
                db.insert("contacts",
                        null,
                        values);
            }
        }

        Log.d("CodeplugDB","Inserted "+getContactCount()+" rows of contacts.");


        //Write the codeplug image to disk, so it's consistent for the next load.
        writeCodeplug();
    }

    /* Returns the number of contacts. */
    public int getContactCount(){
        Cursor c=db.rawQuery("select count(*) from contacts",null);
        c.moveToFirst();
        return c.getInt(0);
    }
    /* Returns the name of a contact. */
    public MD380Contact getContact(int adr){
        Cursor c=db.rawQuery("select id, llid, flag, name from contacts where id="+adr,null);
        //Cursor c=db.rawQuery("select 1 as id, 42 as llid, 0 as flag, 'dude' as name;",null);
        if(c.moveToFirst())
            return new MD380Contact(c);
        else
            return null;
    }

    public void writeCodeplug(){
        FileOutputStream fos;
        try{
            fos=context.openFileOutput("codeplug.img",Context.MODE_PRIVATE);
            fos.write(codeplug.getImage());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /* This export the SQLite database to a codeplug file. */
    public MD380Codeplug exportCodeplug(){
        writeCodeplug();
        return codeplug;
    }
}
