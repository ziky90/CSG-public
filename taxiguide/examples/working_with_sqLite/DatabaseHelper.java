package com.citysmartgo.android.taxiguide.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.citysmartgo.android.taxiguide.model.Discount;
import com.citysmartgo.android.taxiguide.model.TaxiCompany;

/**
 * 
 * @author matyama, zikesjan
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	/**
	 * android's default system path for application database.
	 */
	public static final String DB_PATH = "/data/data/com.citysmartgo.android.taxiguide/databases/";

	public static final String DB_NAME = "companies"; 

	public static final String COMPANIES_TB_NAME = "companies";

	public static final String COMPANIES_COL_ID = "_id";

	public static final String COMPANIES_COL_NAME = "name";

	public static final String COMPANIES_COL_PHONE = "number";

	public static final String COMPANIES_COL_PRICE_PER_UNIT = "price_per_unit";

	public static final String COMPANIES_COL_BASE_FARE = "entering_price";

	public static final String COMPANIES_COL_WAITING_PRICE = "waiting_price";

	public static final String COMPANIES_COL_CITY = "city";

	public static final String[] COMPANIES_COLUMNS = { COMPANIES_COL_ID,
			COMPANIES_COL_NAME, COMPANIES_COL_PHONE,
			COMPANIES_COL_PRICE_PER_UNIT, COMPANIES_COL_BASE_FARE,
			COMPANIES_COL_WAITING_PRICE, COMPANIES_COL_CITY };

	public static final String DISCOUNTS_TB_NAME = "discounts";

	public static final String DISCOUNTS_COL_ID = "_id";

	public static final String DISCOUNTS_COL_NAME = "name";

	public static final String DISCOUNTS_COL_COMPANY = "company";

	public static final String DISCOUNTS_COL_PRICE_PER_UNIT = "price_per_unit";

	public static final String DISCOUNTS_COL_BASE_FARE = "entering_price";

	public static final String DISCOUNTS_COL_WAITING_PRICE = "waiting_price";

	public static final String[] DISCOUNTS_COLUMNS = { DISCOUNTS_COL_ID,
			DISCOUNTS_COL_NAME, DISCOUNTS_COL_COMPANY,
			DISCOUNTS_COL_PRICE_PER_UNIT, DISCOUNTS_COL_BASE_FARE,
			DISCOUNTS_COL_WAITING_PRICE };

	private SQLiteDatabase db;

	private static Context context;

	private static final int DB_VERSION = 3; // important it is needed to
												// increase this number on every
												// database update!!!!!

	private static volatile DatabaseHelper instance;

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 */
	private DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// close();
		setContext(context);
		createDataBase();
		openDatabase();
	}

	public static DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			synchronized (DatabaseHelper.class) {
				if (instance == null) {
					instance = new DatabaseHelper(context);
				}
			}
		}
		setContext(context);
		return instance;
	}

	private static void setContext(Context context) {
		DatabaseHelper.context = context;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	private void createDataBase() {
		if (checkDatabase()) {
			if (databaseVersion() != DB_VERSION) { // XXX check this more properly
				this.getReadableDatabase();
				openDatabase();
				List<Discount> discounts = getAllDiscounts();
				try {
					copyDatabase();
					saveDiscounts(discounts);
					
				} catch (IOException e) {
					throw new Error("Error copying database");
				}
				finally {
					
				}
			}
		} else {
			// By calling this method and empty database will be created into
			// the default system path
			// of your application so we are gonna be able to overwrite that
			// database with our database.
			this.getReadableDatabase();

			try {
				copyDatabase();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error copying database");
			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDatabase() {
		SQLiteDatabase checkDB = null;
		try {
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		if (checkDB != null) {
			checkDB.close();
		}
		return checkDB != null;
	}

	/**
	 * 
	 * method for checking of the DB version because of the updates
	 */
	private int databaseVersion() {
		int version = 0;
		try {
			String myPath = DB_PATH + DB_NAME;
			SQLiteDatabase db = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);
			version = db.getVersion();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		return version;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transfering bytestream.
	 * */
	private void copyDatabase() throws IOException {

		// Open your local db as the input stream
		InputStream myInput = context.getAssets().open(DB_NAME);

		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile 
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void openDatabase() throws SQLException {
		// Open the database
		String myPath = DB_PATH + DB_NAME;
		db = SQLiteDatabase.openDatabase(myPath, null,
				SQLiteDatabase.OPEN_READWRITE);
	}

	@Override
	public synchronized void close() {
		if (db != null) {
			db.close();
		}
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	
	/**
	 * method for listing all the companies
	 * @return
	 */
	public List<TaxiCompany> getAllCompanies() {
		List<TaxiCompany> companies = new ArrayList<TaxiCompany>();
		try {
			Cursor cursor = db.query(COMPANIES_TB_NAME, COMPANIES_COLUMNS,
					null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				TaxiCompany tc = cursorToTaxiCompany(cursor);
				companies.add(tc);
				cursor.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return companies;
		} catch (Exception e) {
			e.printStackTrace();
			return companies;
		}
		return companies;
	}

	/**
	 * method for getting the given company by id
	 * @param id
	 * @return
	 */
	public TaxiCompany getCompanyById(long id) {
		try {
			Cursor cursor = db.query(COMPANIES_TB_NAME, COMPANIES_COLUMNS,
					COMPANIES_COL_ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null);
			if (!cursor.moveToFirst()) {
				return new TaxiCompany(); 
			}
			return cursorToTaxiCompany(cursor);
		} catch (SQLException e) {
			e.printStackTrace();
			return new TaxiCompany(); 
		} catch (Exception e) {
			e.printStackTrace();
			return new TaxiCompany();
		}
	}

	/**
	 * method for getting companies with all the id's in the Collection
	 * @param ids
	 * @return
	 */
	public List<TaxiCompany> getCompaniesByIds(Collection<Long> ids) {
		List<TaxiCompany> companies = new ArrayList<TaxiCompany>();
		for (Long id : ids) {
			companies.add(getCompanyById(id));
		}
		return companies;
	}

	/**
	 * listing all the companies in the given city
	 * @param city
	 * @return
	 */
	public List<TaxiCompany> getCompaniesByCity(String city) {
		List<TaxiCompany> companies = new ArrayList<TaxiCompany>();
		try {
			Cursor cursor = db.query(COMPANIES_TB_NAME, COMPANIES_COLUMNS,
					COMPANIES_COL_CITY + "=?", new String[] { city }, null,
					null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				TaxiCompany tc = cursorToTaxiCompany(cursor);
				companies.add(tc);
				cursor.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return companies;
		}
		return companies;
	}

	/**
	 * listing all the companies in the given cities
	 * @param cities
	 * @return
	 */
	public List<TaxiCompany> getCompaniesByCities(Collection<String> cities) {
		List<TaxiCompany> companies = new ArrayList<TaxiCompany>();
		for (String city : cities) {
			companies.addAll(getCompaniesByCity(city));
		}
		return companies;
	}

	/**
	 * listing all the discounts
	 * @return
	 */
	public List<Discount> getAllDiscounts() {
		List<Discount> discounts = new ArrayList<Discount>();
		try {
			Cursor cursor = db.query(DISCOUNTS_TB_NAME, DISCOUNTS_COLUMNS,
					null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Discount discount = cursorToDiscount(cursor);
				discounts.add(discount);
				cursor.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return discounts;
		} catch (Exception e) {
			e.printStackTrace();
			return discounts;
		}
		return discounts;
	}

	/**
	 * returning discount corresponding to the given id
	 * @param id
	 * @return
	 */
	public Discount getDiscountById(long id) {
		try {
			Cursor cursor = db.query(DISCOUNTS_TB_NAME, DISCOUNTS_COLUMNS,
					DISCOUNTS_COL_ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null);
			if (!cursor.moveToFirst()) {
				return new Discount(); 
			}
			return cursorToDiscount(cursor);
		} catch (SQLException e) {
			e.printStackTrace();
			return new Discount();
		} catch (Exception e) {
			e.printStackTrace();
			return new Discount(); 
		}
	}

	/**
	 * listing all the discounts with ids from the collection
	 * @param ids
	 * @return
	 */
	public List<Discount> getDiscountsByIds(Collection<Long> ids) {
		List<Discount> discounts = new ArrayList<Discount>();
		for (Long id : ids) {
			discounts.add(getDiscountById(id));
		}
		return discounts;
	}

	/**
	 * listing all the discount within the given company
	 * @param company
	 * @return
	 */
	public List<Discount> getDiscountsByCompany(TaxiCompany company) {
		List<Discount> discounts = new ArrayList<Discount>();
		try {
			Cursor cursor = db.query(DISCOUNTS_TB_NAME, DISCOUNTS_COLUMNS,
					DISCOUNTS_COL_COMPANY + "=?",
					new String[] { String.valueOf(company.getId()) }, null,
					null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Discount discount = cursorToDiscount(cursor);
				discounts.add(discount);
				cursor.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return discounts;
		} catch (Exception e) {
			e.printStackTrace();
			return discounts;
		}
		return discounts;
	}

	/**
	 * listing all the discount within the given companys
	 * @param companies
	 * @return
	 */
	public List<Discount> getDiscountsByCompanies(
			Collection<TaxiCompany> companies) {
		List<Discount> discounts = new ArrayList<Discount>();
		for (TaxiCompany company : companies) {
			discounts.addAll(getDiscountsByCompany(company));
		}
		return discounts;
	}

	/**
	 * listing discounts for the given city
	 * @param city
	 * @return
	 */
	public List<Discount> getDiscountsByCity(String city) {
		return getDiscountsByCompanies(getCompaniesByCity(city));
	}

	/**
	 * listing discounts for the given cities
	 * @param city
	 * @return
	 */
	public List<Discount> getDiscountsByCities(Collection<String> cities) {
		return getDiscountsByCompanies(getCompaniesByCities(cities));
	}

	/**
	 * saving discount created by the user
	 * @param discount
	 * @return
	 */
	public long saveDiscount(Discount discount) {
		
		ContentValues values = new ContentValues();
		values.put(DISCOUNTS_COL_NAME, discount.getName());
		values.put(DISCOUNTS_COL_COMPANY, discount.getCompany().getId());
		values.put(DISCOUNTS_COL_PRICE_PER_UNIT, discount.getPricePerUnit());
		values.put(DISCOUNTS_COL_BASE_FARE, discount.getEnteringPrice());
		values.put(DISCOUNTS_COL_WAITING_PRICE, discount.getWaitingPrice());

		return db.insert(DISCOUNTS_TB_NAME, null, values);
	}
	
	/**
	 * saving multiple discounts created at once applicable when there is db update
	 * @param discounts
	 */
	public void saveDiscounts(Collection<Discount> discounts) {
		for (Discount discount : discounts) {
			saveDiscount(discount);
		}
	}
	
	/**
	 * updating of the changed discount
	 * @param discount
	 * @return
	 */
	public long updateDiscount(Discount discount) {
		
		ContentValues values = new ContentValues();
		values.put(DISCOUNTS_COL_ID, discount.getId());
		values.put(DISCOUNTS_COL_NAME, discount.getName());
		values.put(DISCOUNTS_COL_COMPANY, discount.getCompany().getId());
		values.put(DISCOUNTS_COL_PRICE_PER_UNIT, discount.getPricePerUnit());
		values.put(DISCOUNTS_COL_BASE_FARE, discount.getEnteringPrice());
		values.put(DISCOUNTS_COL_WAITING_PRICE, discount.getWaitingPrice());

		return db.replace(DISCOUNTS_TB_NAME, null, values);
	}

	/**
	 * deleting discount from the db
	 * @param id
	 * @return
	 */
	public boolean deleteDiscount(long id) {
		return db.delete(DISCOUNTS_TB_NAME, DISCOUNTS_COL_ID + "=?",
				new String[] { String.valueOf(id) }) > 0;
	}

	private TaxiCompany cursorToTaxiCompany(Cursor cursor) {
		return new TaxiCompany(cursor.getLong(0), cursor.getString(1),
				cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4),
				cursor.getDouble(5), cursor.getString(6));
	}

	private Discount cursorToDiscount(Cursor cursor) {
		return new Discount(cursor.getLong(0), cursor.getString(1),
				getCompanyById(cursor.getLong(2)), cursor.getDouble(3),
				cursor.getDouble(4), cursor.getDouble(5));
	}

}
