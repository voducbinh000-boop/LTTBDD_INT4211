package com.example.quanlychitieu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý toàn bộ Cơ sở dữ liệu SQLite:
 * - Người dùng (users)
 * - Danh mục (categories) - gắn liền với user_id
 * - Giao dịch (transactions)
 * - Ngân sách (budgets) - theo tháng & theo từng danh mục
 * - Mục tiêu tiết kiệm (savings_goals)
 * - Giao dịch định kỳ (recurring_transactions)
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "quanlychitieu.db";
    public static final int DB_VERSION = 2;
    private static DBHelper instance;

    public static synchronized DBHelper get(Context c) {
        if (instance == null) instance = new DBHelper(c.getApplicationContext());
        return instance;
    }

    private DBHelper(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Bảng người dùng
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "email TEXT UNIQUE, " +
                "password TEXT, " +
                "firebase_uid TEXT)");

        // 2. Bảng danh mục thu/chi
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER DEFAULT 0, " +
                "name TEXT, " +
                "type TEXT, " +
                "icon TEXT)");

        // 3. Bảng giao dịch thu/chi
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "type TEXT, " +
                "category_id INTEGER, " +
                "amount INTEGER, " +
                "note TEXT, " +
                "date TEXT)");

        // 4. Bảng quản lý ngân sách (theo tháng yyyy-MM & category_id)
        db.execSQL("CREATE TABLE budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "month TEXT, " +
                "amount INTEGER, " +
                "category_id INTEGER DEFAULT 0)");

        // 5. Bảng mục tiêu tiết kiệm
        db.execSQL("CREATE TABLE savings_goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "name TEXT, " +
                "target_amount INTEGER, " +
                "current_amount INTEGER DEFAULT 0, " +
                "start_date TEXT, " +
                "end_date TEXT, " +
                "status TEXT DEFAULT 'active')");

        // 6. Bảng giao dịch định kỳ (tự động nhắc và tạo giao dịch khi đến hạn)
        db.execSQL("CREATE TABLE recurring_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "type TEXT, " +
                "category_id INTEGER, " +
                "amount INTEGER, " +
                "note TEXT, " +
                "start_date TEXT, " +
                "frequency TEXT, " +
                "next_date TEXT, " +
                "is_active INTEGER DEFAULT 1)");

        // Dữ liệu danh mục mặc định ban đầu (user_id = 0)
        seedCategory(db, 0, "Ăn uống", "expense", "food");
        seedCategory(db, 0, "Mua sắm", "expense", "shopping");
        seedCategory(db, 0, "Đi lại", "expense", "transport");
        seedCategory(db, 0, "Hóa đơn", "expense", "other");
        seedCategory(db, 0, "Tiết kiệm", "expense", "wallet");
        seedCategory(db, 0, "Học tập", "expense", "other");
        seedCategory(db, 0, "Sức khỏe", "expense", "other");
        seedCategory(db, 0, "Khác", "expense", "other");

        seedCategory(db, 0, "Tiền lương", "income", "salary");
        seedCategory(db, 0, "Tiền thưởng", "income", "wallet");
        seedCategory(db, 0, "Đầu tư", "income", "wallet");
        seedCategory(db, 0, "Thu nhập khác", "income", "other");
    }

    private void seedCategory(SQLiteDatabase db, long userId, String name, String type, String icon) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("name", name);
        v.put("type", type);
        v.put("icon", icon);
        db.insert("categories", null, v);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Cập nhật cấu trúc bảng từ phiên bản 1 lên phiên bản 2
            try {
                db.execSQL("ALTER TABLE categories ADD COLUMN user_id INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN firebase_uid TEXT");
            } catch (Exception ignored) {}

            db.execSQL("CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "month TEXT, " +
                    "amount INTEGER, " +
                    "category_id INTEGER DEFAULT 0)");

            db.execSQL("CREATE TABLE IF NOT EXISTS savings_goals (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "name TEXT, " +
                    "target_amount INTEGER, " +
                    "current_amount INTEGER DEFAULT 0, " +
                    "start_date TEXT, " +
                    "end_date TEXT, " +
                    "status TEXT DEFAULT 'active')");

            db.execSQL("CREATE TABLE IF NOT EXISTS recurring_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "type TEXT, " +
                    "category_id INTEGER, " +
                    "amount INTEGER, " +
                    "note TEXT, " +
                    "start_date TEXT, " +
                    "frequency TEXT, " +
                    "next_date TEXT, " +
                    "is_active INTEGER DEFAULT 1)");
        }
    }

    // ==========================================
    // 1. NGƯỜI DÙNG & XÁC THỰC (AUTHENTICATION)
    // ==========================================

    public long register(String name, String email, String password) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("email", email.toLowerCase().trim());
        v.put("password", Utils.hash(password));
        return getWritableDatabase().insert("users", null, v);
    }

    public User login(String identifier, String password) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, email, firebase_uid FROM users " +
                        "WHERE (LOWER(email)=LOWER(?) OR LOWER(name)=LOWER(?)) AND password=?",
                new String[]{identifier.trim(), identifier.trim(), Utils.hash(password)});
        User u = null;
        if (c.moveToFirst()) u = readUser(c);
        c.close();
        return u;
    }

    public User getUser(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, email, firebase_uid FROM users WHERE id=?",
                new String[]{String.valueOf(id)});
        User u = null;
        if (c.moveToFirst()) u = readUser(c);
        c.close();
        return u;
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, email, firebase_uid FROM users WHERE LOWER(email)=LOWER(?)",
                new String[]{email.trim()});
        User u = null;
        if (c.moveToFirst()) u = readUser(c);
        c.close();
        return u;
    }

    /** Đồng bộ tài khoản đăng nhập từ Firebase Google / Email vào SQLite cục bộ */
    public User syncFirebaseUser(String name, String email, String firebaseUid) {
        User existing = getUserByEmail(email);
        if (existing != null) {
            ContentValues v = new ContentValues();
            if (name != null && !name.isEmpty()) v.put("name", name);
            if (firebaseUid != null) v.put("firebase_uid", firebaseUid);
            getWritableDatabase().update("users", v, "id=?", new String[]{String.valueOf(existing.id)});
            return getUser(existing.id);
        } else {
            ContentValues v = new ContentValues();
            v.put("name", name != null && !name.isEmpty() ? name : email);
            v.put("email", email.toLowerCase().trim());
            v.put("password", Utils.hash("firebase_auth_" + firebaseUid));
            v.put("firebase_uid", firebaseUid);
            long newId = getWritableDatabase().insert("users", null, v);
            return getUser(newId);
        }
    }

    public boolean resetPassword(String email, String newPassword) {
        ContentValues v = new ContentValues();
        v.put("password", Utils.hash(newPassword));
        int n = getWritableDatabase().update("users", v, "LOWER(email)=LOWER(?)",
                new String[]{email.trim()});
        return n > 0;
    }

    public boolean changePassword(long userId, String oldPass, String newPass) {
        ContentValues v = new ContentValues();
        v.put("password", Utils.hash(newPass));
        int n = getWritableDatabase().update("users", v, "id=? AND password=?",
                new String[]{String.valueOf(userId), Utils.hash(oldPass)});
        return n > 0;
    }

    private User readUser(Cursor c) {
        User u = new User();
        u.id = c.getLong(0);
        u.name = c.getString(1);
        u.email = c.getString(2);
        u.firebaseUid = c.getString(3);
        return u;
    }

    // ==========================================
    // 2. DANH MỤC (CATEGORIES) - QUẢN LÝ THEO USER
    // ==========================================

    /** Lấy danh mục mặc định (user_id=0) và danh mục riêng của user đó */
    public List<Category> getCategories(long userId, String type) {
        List<Category> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, user_id, name, type, icon FROM categories " +
                        "WHERE (user_id=0 OR user_id=?) AND type=? ORDER BY user_id ASC, id ASC",
                new String[]{String.valueOf(userId), type});
        while (c.moveToNext()) {
            list.add(new Category(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4)));
        }
        c.close();
        return list;
    }

    /** Tương thích ngược: khi không truyền userId, mặc định lấy userId hiện tại hoặc 0 */
    public List<Category> getCategories(String type) {
        return getCategories(0, type);
    }

    public long addCategory(long userId, String name, String type, String icon) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("name", name);
        v.put("type", type);
        v.put("icon", icon != null ? icon : "other");
        return getWritableDatabase().insert("categories", null, v);
    }

    public void addCategory(String name, String type) {
        addCategory(0, name, type, "other");
    }

    public boolean updateCategory(long id, long userId, String newName, String newIcon) {
        ContentValues v = new ContentValues();
        v.put("name", newName);
        if (newIcon != null) v.put("icon", newIcon);
        int rows = getWritableDatabase().update("categories", v,
                "id=? AND (user_id=? OR user_id=0)",
                new String[]{String.valueOf(id), String.valueOf(userId)});
        return rows > 0;
    }

    /** Không cho xóa danh mục đang có giao dịch phát sinh */
    public boolean deleteCategory(long id, long userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM transactions WHERE category_id=?",
                new String[]{String.valueOf(id)});
        c.moveToFirst();
        int used = c.getInt(0);
        c.close();
        if (used > 0) return false;

        // Chỉ cho phép xóa nếu là danh mục do chính user tạo, hoặc nếu là admin
        int n = getWritableDatabase().delete("categories", "id=?", new String[]{String.valueOf(id)});
        return n > 0;
    }

    public boolean deleteCategory(long id) {
        return deleteCategory(id, 0);
    }

    // ==========================================
    // 3. GIAO DỊCH (TRANSACTIONS)
    // ==========================================

    public void insertTransaction(long userId, Transaction t) {
        getWritableDatabase().insert("transactions", null, values(userId, t));
    }

    public void updateTransaction(long userId, Transaction t) {
        getWritableDatabase().update("transactions", values(userId, t), "id=?",
                new String[]{String.valueOf(t.id)});
    }

    public void deleteTransaction(long id) {
        getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(id)});
    }

    private ContentValues values(long userId, Transaction t) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("type", t.type);
        v.put("category_id", t.categoryId);
        v.put("amount", t.amount);
        v.put("note", t.note);
        v.put("date", t.date);
        return v;
    }

    private static final String SELECT_TX =
            "SELECT t.id, t.type, t.category_id, t.amount, t.note, t.date, c.name, c.icon " +
                    "FROM transactions t LEFT JOIN categories c ON t.category_id=c.id ";

    private Transaction readTx(Cursor c) {
        Transaction t = new Transaction();
        t.id = c.getLong(0);
        t.type = c.getString(1);
        t.categoryId = c.getLong(2);
        t.amount = c.getLong(3);
        t.note = c.getString(4);
        t.date = c.getString(5);
        t.categoryName = c.getString(6);
        t.categoryIcon = c.getString(7);
        return t;
    }

    public Transaction getTransaction(long id) {
        Cursor c = getReadableDatabase().rawQuery(SELECT_TX + "WHERE t.id=?",
                new String[]{String.valueOf(id)});
        Transaction t = null;
        if (c.moveToFirst()) t = readTx(c);
        c.close();
        return t;
    }

    public List<Transaction> getTransactions(long userId, String type, String keyword, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_TX + "WHERE t.user_id=?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));
        if (type != null) {
            sql.append(" AND t.type=?");
            args.add(type);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (t.note LIKE ? OR c.name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY t.date DESC, t.id DESC");
        if (limit > 0) sql.append(" LIMIT ").append(limit);

        List<Transaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) list.add(readTx(c));
        c.close();
        return list;
    }

    /** Lọc giao dịch nâng cao: Từ ngày - Đến ngày, danh mục, từ khóa */
    public List<Transaction> getTransactionsFiltered(long userId, String type, String fromDate,
                                                    String toDate, Long categoryId, String keyword) {
        StringBuilder sql = new StringBuilder(SELECT_TX + "WHERE t.user_id=?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            sql.append(" AND t.type=?");
            args.add(type);
        }
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append(" AND t.date>=?");
            args.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND t.date<=?");
            args.add(toDate);
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND t.category_id=?");
            args.add(String.valueOf(categoryId));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (t.note LIKE ? OR c.name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY t.date DESC, t.id DESC");

        List<Transaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) list.add(readTx(c));
        c.close();
        return list;
    }

    public long sum(long userId, String type, String from, String to) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                        "WHERE user_id=? AND type=? AND date>=? AND date<=?",
                new String[]{String.valueOf(userId), type, from, to});
        c.moveToFirst();
        long v = c.getLong(0);
        c.close();
        return v;
    }

    public List<CategorySum> expenseByCategory(long userId, String from, String to) {
        List<CategorySum> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(c.name, 'Khác'), c.icon, SUM(t.amount) s " +
                        "FROM transactions t LEFT JOIN categories c ON t.category_id=c.id " +
                        "WHERE t.user_id=? AND t.type='expense' AND t.date>=? AND t.date<=? " +
                        "GROUP BY t.category_id ORDER BY s DESC",
                new String[]{String.valueOf(userId), from, to});
        while (c.moveToNext()) {
            CategorySum s = new CategorySum();
            s.name = c.getString(0);
            s.icon = c.getString(1);
            s.total = c.getLong(2);
            list.add(s);
        }
        c.close();
        return list;
    }

    public long[][] monthlyTotals(long userId, int year) {
        long[][] r = new long[2][12];
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT CAST(substr(date, 6, 2) AS INTEGER), type, SUM(amount) FROM transactions " +
                        "WHERE user_id=? AND substr(date, 1, 4)=? GROUP BY 1, 2",
                new String[]{String.valueOf(userId), String.valueOf(year)});
        while (c.moveToNext()) {
            int m = c.getInt(0) - 1;
            if (m < 0 || m > 11) continue;
            r["income".equals(c.getString(1)) ? 0 : 1][m] = c.getLong(2);
        }
        c.close();
        return r;
    }

    /**
     * So sánh chi tiêu và thu nhập với tháng trước.
     * Trả về mảng 4 phần tử: [0] Chi tiêu tháng này, [1] Chi tiêu tháng trước, [2] Thu tháng này, [3] Thu tháng trước.
     */
    public long[] getMonthlyComparison(long userId, int year, int month) {
        String curFrom = Utils.toIso(year, month, 1);
        String curTo = Utils.toIso(year, month, 31);

        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        String prevFrom = Utils.toIso(prevYear, prevMonth, 1);
        String prevTo = Utils.toIso(prevYear, prevMonth, 31);

        long curExpense = sum(userId, "expense", curFrom, curTo);
        long prevExpense = sum(userId, "expense", prevFrom, prevTo);
        long curIncome = sum(userId, "income", curFrom, curTo);
        long prevIncome = sum(userId, "income", prevFrom, prevTo);

        return new long[]{curExpense, prevExpense, curIncome, prevIncome};
    }

    // ==========================================
    // 4. QUẢN LÝ NGÂN SÁCH (BUDGETS)
    // ==========================================

    public void setBudget(long userId, String month, long categoryId, long amount) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM budgets WHERE user_id=? AND month=? AND category_id=?",
                new String[]{String.valueOf(userId), month, String.valueOf(categoryId)});
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("month", month);
        v.put("category_id", categoryId);
        v.put("amount", amount);

        if (c.moveToFirst()) {
            long id = c.getLong(0);
            db.update("budgets", v, "id=?", new String[]{String.valueOf(id)});
        } else {
            db.insert("budgets", null, v);
        }
        c.close();
    }

    public long getTotalMonthlyBudget(long userId, String month) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT amount FROM budgets WHERE user_id=? AND month=? AND (category_id=0 OR category_id IS NULL)",
                new String[]{String.valueOf(userId), month});
        long amount = 0;
        if (c.moveToFirst()) amount = c.getLong(0);
        c.close();
        return amount;
    }

    public List<Budget> getBudgetsForMonth(long userId, String month) {
        List<Budget> list = new ArrayList<>();
        // Query toàn bộ ngân sách trong tháng của user
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT b.id, b.user_id, b.month, b.amount, b.category_id, c.name, c.icon " +
                        "FROM budgets b LEFT JOIN categories c ON b.category_id=c.id " +
                        "WHERE b.user_id=? AND b.month=? ORDER BY b.category_id ASC",
                new String[]{String.valueOf(userId), month});

        String from = month + "-01";
        String to = month + "-31";

        while (c.moveToNext()) {
            Budget b = new Budget();
            b.id = c.getLong(0);
            b.userId = c.getLong(1);
            b.month = c.getString(2);
            b.amount = c.getLong(3);
            b.categoryId = c.getLong(4);
            b.categoryName = c.getString(5);
            b.categoryIcon = c.getString(6);

            // Tính số tiền đã chi thực tế trong tháng
            if (b.categoryId == 0) {
                b.spentAmount = sum(userId, "expense", from, to);
                b.categoryName = "Tổng ngân sách tháng";
            } else {
                Cursor sc = getReadableDatabase().rawQuery(
                        "SELECT COALESCE(SUM(amount),0) FROM transactions " +
                                "WHERE user_id=? AND type='expense' AND category_id=? AND date>=? AND date<=?",
                        new String[]{String.valueOf(userId), String.valueOf(b.categoryId), from, to});
                if (sc.moveToFirst()) b.spentAmount = sc.getLong(0);
                sc.close();
            }
            list.add(b);
        }
        c.close();
        return list;
    }

    public boolean deleteBudget(long id, long userId) {
        int n = getWritableDatabase().delete("budgets", "id=? AND user_id=?",
                new String[]{String.valueOf(id), String.valueOf(userId)});
        return n > 0;
    }

    // ==========================================
    // 5. MỤC TIÊU TIẾT KIỆM (SAVINGS GOALS)
    // ==========================================

    public List<SavingsGoal> getSavingsGoals(long userId) {
        List<SavingsGoal> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, user_id, name, target_amount, current_amount, start_date, end_date, status " +
                        "FROM savings_goals WHERE user_id=? ORDER BY id DESC",
                new String[]{String.valueOf(userId)});
        while (c.moveToNext()) {
            SavingsGoal g = new SavingsGoal(
                    c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3),
                    c.getLong(4), c.getString(5), c.getString(6), c.getString(7)
            );
            list.add(g);
        }
        c.close();
        return list;
    }

    public long insertSavingsGoal(long userId, SavingsGoal g) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("name", g.name);
        v.put("target_amount", g.targetAmount);
        v.put("current_amount", g.currentAmount);
        v.put("start_date", g.startDate);
        v.put("end_date", g.endDate);
        v.put("status", g.isCompleted() ? "completed" : "active");
        return getWritableDatabase().insert("savings_goals", null, v);
    }

    public boolean updateSavingsGoal(SavingsGoal g) {
        ContentValues v = new ContentValues();
        v.put("name", g.name);
        v.put("target_amount", g.targetAmount);
        v.put("current_amount", g.currentAmount);
        v.put("start_date", g.startDate);
        v.put("end_date", g.endDate);
        v.put("status", g.isCompleted() ? "completed" : "active");
        int n = getWritableDatabase().update("savings_goals", v, "id=?",
                new String[]{String.valueOf(g.id)});
        return n > 0;
    }

    /** Nạp thêm tiền vào mục tiêu tiết kiệm */
    public boolean addSavingsAmount(long goalId, long addAmount) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT target_amount, current_amount FROM savings_goals WHERE id=?",
                new String[]{String.valueOf(goalId)});
        if (!c.moveToFirst()) {
            c.close();
            return false;
        }
        long target = c.getLong(0);
        long current = c.getLong(1) + addAmount;
        c.close();

        ContentValues v = new ContentValues();
        v.put("current_amount", current);
        if (current >= target) v.put("status", "completed");
        int n = getWritableDatabase().update("savings_goals", v, "id=?",
                new String[]{String.valueOf(goalId)});
        return n > 0;
    }

    public boolean deleteSavingsGoal(long goalId, long userId) {
        int n = getWritableDatabase().delete("savings_goals", "id=? AND user_id=?",
                new String[]{String.valueOf(goalId), String.valueOf(userId)});
        return n > 0;
    }

    // ==========================================
    // 6. GIAO DỊCH ĐỊNH KỲ (RECURRING TRANSACTIONS)
    // ==========================================

    public List<RecurringTransaction> getRecurringTransactions(long userId) {
        List<RecurringTransaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT r.id, r.user_id, r.type, r.category_id, r.amount, r.note, " +
                        "r.start_date, r.frequency, r.next_date, r.is_active, c.name, c.icon " +
                        "FROM recurring_transactions r LEFT JOIN categories c ON r.category_id=c.id " +
                        "WHERE r.user_id=? ORDER BY r.next_date ASC",
                new String[]{String.valueOf(userId)});
        while (c.moveToNext()) {
            RecurringTransaction r = new RecurringTransaction();
            r.id = c.getLong(0);
            r.userId = c.getLong(1);
            r.type = c.getString(2);
            r.categoryId = c.getLong(3);
            r.amount = c.getLong(4);
            r.note = c.getString(5);
            r.startDate = c.getString(6);
            r.frequency = c.getString(7);
            r.nextDate = c.getString(8);
            r.isActive = c.getInt(9) == 1;
            r.categoryName = c.getString(10);
            r.categoryIcon = c.getString(11);
            list.add(r);
        }
        c.close();
        return list;
    }

    public long insertRecurringTransaction(long userId, RecurringTransaction r) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("type", r.type);
        v.put("category_id", r.categoryId);
        v.put("amount", r.amount);
        v.put("note", r.note);
        v.put("start_date", r.startDate);
        v.put("frequency", r.frequency);
        v.put("next_date", r.nextDate);
        v.put("is_active", r.isActive ? 1 : 0);
        return getWritableDatabase().insert("recurring_transactions", null, v);
    }

    public boolean updateRecurringTransaction(RecurringTransaction r) {
        ContentValues v = new ContentValues();
        v.put("type", r.type);
        v.put("category_id", r.categoryId);
        v.put("amount", r.amount);
        v.put("note", r.note);
        v.put("frequency", r.frequency);
        v.put("next_date", r.nextDate);
        v.put("is_active", r.isActive ? 1 : 0);
        int n = getWritableDatabase().update("recurring_transactions", v, "id=?",
                new String[]{String.valueOf(r.id)});
        return n > 0;
    }

    public boolean deleteRecurringTransaction(long id, long userId) {
        int n = getWritableDatabase().delete("recurring_transactions", "id=? AND user_id=?",
                new String[]{String.valueOf(id), String.valueOf(userId)});
        return n > 0;
    }

    /**
     * Tự động quét và phát sinh giao dịch thật vào bảng transactions khi đến hạn.
     * Đảm bảo không tạo trùng lặp và tự động cập nhật ngày đến kỳ kế tiếp.
     */
    public int processDueRecurringTransactions(long userId) {
        String today = Utils.today();
        List<RecurringTransaction> list = getRecurringTransactions(userId);
        int generatedCount = 0;

        for (RecurringTransaction r : list) {
            if (!r.isActive) continue;
            // Nếu ngày đến hạn nhỏ hơn hoặc bằng hôm nay -> tiến hành tạo giao dịch
            if (r.nextDate != null && r.nextDate.compareTo(today) <= 0) {
                Transaction t = new Transaction();
                t.type = r.type;
                t.categoryId = r.categoryId;
                t.amount = r.amount;
                t.note = (r.note == null || r.note.isEmpty() ? "Giao dịch định kỳ" : r.note) + " (Tự động)";
                t.date = r.nextDate;

                insertTransaction(userId, t);
                generatedCount++;

                // Tính toán ngày của kỳ kế tiếp
                String next = RecurringTransaction.computeNextDate(r.nextDate, r.frequency);
                r.nextDate = next;
                updateRecurringTransaction(r);
            }
        }
        return generatedCount;
    }
}
