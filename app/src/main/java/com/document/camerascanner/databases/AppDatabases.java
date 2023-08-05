package com.document.camerascanner.databases;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.document.camerascanner.databases.dao.DocumentDao;
import com.document.camerascanner.databases.dao.FolderDao;
import com.document.camerascanner.databases.dao.PageDao;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.FolderItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.utils.Constants;

import java.lang.ref.WeakReference;

@Database(entities = {FolderItem.class, DocumentItem.class, PageItem.class}, version = Constants.DATABASES_VERSION, exportSchema = false)
public abstract class AppDatabases extends RoomDatabase {

    private static final String TRIGGER_COUNT_DOWN_PAGE = "CREATE TRIGGER IF NOT EXISTS TRIGGER_COUNT_DOWN_PAGE " +
            "AFTER DELETE ON tblPage " +
            "BEGIN UPDATE tblDocument SET  child_count =  child_count - 1, size =  size - OLD.size WHERE id = OLD.parent_id;  END;";

    private static final String TRIGGER_COUNT_UP_PAGE = "CREATE TRIGGER IF NOT EXISTS TRIGGER_COUNT_UP_PAGE " +
            "AFTER INSERT ON tblPage " +
            "BEGIN UPDATE tblDocument SET child_count =  child_count + 1, size =  size + NEW.size WHERE id = NEW.parent_id;  END;";

    private static final String TRIGGER_BEFORE_UPDATE_PAGE = "CREATE TRIGGER IF NOT EXISTS TRIGGER_BEFORE_UPDATE_PAGE " +
            "BEFORE UPDATE ON tblPage " +
            "BEGIN UPDATE tblDocument SET child_count =  child_count - 1, size =  size - OLD.size WHERE id = OLD.parent_id;  END;";

    private static final String TRIGGER_AFTER_UPDATE_PAGE = "CREATE TRIGGER IF NOT EXISTS TRIGGER_AFTER_UPDATE_PAGE " +
            "AFTER UPDATE ON tblPage " +
            "BEGIN UPDATE tblDocument SET child_count =  child_count + 1, size =  size + NEW.size WHERE id = NEW.parent_id;  END;";

    private static final String TRIGGER_COUNT_DOWN_DOC = "CREATE TRIGGER IF NOT EXISTS TRIGGER_COUNT_DOWN_DOC " +
            "AFTER DELETE ON tblDocument " +
            "BEGIN UPDATE tblFolder SET  child_count =  child_count - 1, size =  size - OLD.size WHERE id = OLD.parent_id;  END;";

    private static final String TRIGGER_COUNT_UP_DOC = "CREATE TRIGGER IF NOT EXISTS TRIGGER_COUNT_UP_DOC " +
            "AFTER INSERT ON tblDocument " +
            "BEGIN UPDATE tblFolder SET child_count =  child_count + 1, size =  size + NEW.size WHERE tblFolder.id = NEW.parent_id;  END;";

    private static final String TRIGGER_BEFORE_UPDATE_DOCUMENT = "CREATE TRIGGER IF NOT EXISTS TRIGGER_BEFORE_UPDATE_DOCUMENT " +
            "BEFORE UPDATE ON tblDocument " +
            "BEGIN UPDATE tblFolder SET child_count =  child_count - 1, size =  size - OLD.size WHERE id = OLD.parent_id;  END;";

    private static final String TRIGGER_AFTER_UPDATE_DOCUMENT = "CREATE TRIGGER IF NOT EXISTS TRIGGER_AFTER_UPDATE_DOCUMENT " +
            "AFTER UPDATE ON tblDocument " +
            "BEGIN UPDATE tblFolder SET child_count =  child_count + 1, size =  size + NEW.size WHERE id = NEW.parent_id;  END;";

    private static final String TRIGGER_DELETE_FOLDER_CHILDRENT = "CREATE TRIGGER IF NOT EXISTS TRIGGER_DELETE_FOLDER_CHILDRENT " +
            "AFTER DELETE ON tblFolder " +
            "BEGIN DELETE FROM tblDocument WHERE tblDocument.parent_id = OLD.id;  END";

    private static final String TRIGGER_DELETE_DOCUMENT_CHILDRENT = "CREATE TRIGGER IF NOT EXISTS TRIGGER_DELETE_DOCUMENT_CHILDRENT " +
            "AFTER DELETE ON tblDocument " +
            "BEGIN DELETE FROM tblPage WHERE tblPage.parent_id = OLD.id;  END";

    private static AppDatabases instance;

    public static AppDatabases getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(new WeakReference<>(context).get(), AppDatabases.class, Constants.DATABASE_NAME)
                    .allowMainThreadQueries()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            db.execSQL(TRIGGER_COUNT_UP_DOC);
                            db.execSQL(TRIGGER_COUNT_UP_PAGE);
                            db.execSQL(TRIGGER_COUNT_DOWN_PAGE);
                            db.execSQL(TRIGGER_COUNT_DOWN_DOC);
                            db.execSQL(TRIGGER_BEFORE_UPDATE_PAGE);
                            db.execSQL(TRIGGER_AFTER_UPDATE_PAGE);
                            db.execSQL(TRIGGER_BEFORE_UPDATE_DOCUMENT);
                            db.execSQL(TRIGGER_AFTER_UPDATE_DOCUMENT);
                            db.execSQL(TRIGGER_DELETE_FOLDER_CHILDRENT);
                            db.execSQL(TRIGGER_DELETE_DOCUMENT_CHILDRENT);
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                        }
                    }).build();
        }
        return instance;
    }

    public abstract DocumentDao documentDao();

    public abstract FolderDao folderDao();

    public abstract PageDao pageDao();

}
