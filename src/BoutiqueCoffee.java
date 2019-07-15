import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

class BoutiqueCoffee implements ITransactionManager
{
	private Connection conn;
	private Consumer<String> error_logger;
	
	public BoutiqueCoffee(String url, String username, String password) throws SQLException, ClassNotFoundException {
		// verify that postgresql driver is available
		Class.forName("org.postgresql.Driver");
		
		// create connection to the database
		Properties props = new Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		conn = DriverManager.getConnection(url, props);
		
		// no error logging by default
		setErrorLogger(s -> {});
		addProcedures();
	}
	
	public BoutiqueCoffee(String username, String password) throws SQLException, ClassNotFoundException {
		this("jdbc:postgresql://localhost/postgres", username, password);
	}
	
	public void setErrorLogger(Consumer<String> error_logger) {
		this.error_logger = error_logger;
	}
	
	// TRANSACTIONS

	@Override
	public int addStore(String name, String address, String storeType, double gpsLong, double gpsLat) {
		// TODO Jay
		return 0;
	}

	@Override
	public int addCoffee(String name, String description, int intensity, double price, double rewardPoints,
			double redeemPoints) {
		int id = -1;
		String queryString = "INSERT INTO boutique_coffee.coffee(name, description, intensity, price, reward_points, redeem_points) VALUES (?, ?, ?, ?, ?, ?)";
		String fieldName = "coffee_id";
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(queryString, new String[] {fieldName});
			stmt.setString(1, name);
			stmt.setString(2, description);
			stmt.setInt(3, intensity);
			stmt.setDouble(4, price);
			stmt.setDouble(5, rewardPoints);
			stmt.setDouble(6, redeemPoints);
			int rows = stmt.executeUpdate();
			
			if(rows == 0) {
				throw new SQLException("Add Coffee Failed, no rows affected");
			}
			
			ResultSet values = stmt.getGeneratedKeys();
			if(values.next()) {
				id = values.getInt(1);
			}
		} catch(SQLException e) {
			logException(e);
			id = -1;
		} catch(Exception e) {
			id = -1;
		}
		
		return id;
	}

	@Override
	public int offerCoffee(int storeId, int coffeeId) {
		String insertOfferCoffee = "INSERT INTO boutique_coffee.offercoffee(store_Id, coffee_Id) VALUES(?, ?)";
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement(insertOfferCoffee);
			statement.setInt(1, storeId);
			statement.setInt(2, coffeeId);
			int affectedRows = statement.executeUpdate();

			if(affectedRows == 0) {
				throw new SQLException("Add offerCoffee Failed, no rows affected");
			} else {
				return 1;
			}

		} catch (SQLException e) {
			logException(e);
			return -1;
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public int addPromotion(String name, Date startDate, Date endDate) {
		int id = -1;
		String queryString = "INSERT INTO boutique_coffee.promotion(name, start_date, end_date) VALUES (?, ?, ?)";
		String fieldName = "promotion_id";
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(queryString, new String[] {fieldName});
			stmt.setString(1, name);
			stmt.setDate(2, startDate);
			stmt.setDate(3, endDate);
			int rows = stmt.executeUpdate();
			
			if(rows == 0) {
				throw new SQLException("Add Promotion Failed, no rows affected");
			}
			
			ResultSet values = stmt.getGeneratedKeys();
			if(values.next()) {
				id = values.getInt(1);
			}
		} catch(SQLException e) {
			logException(e);
			id = -1;
		} catch(Exception e) {
			id = -1;
		}
		
		return id;
	}

	@Override
	public int promoteFor(int promotionId, int coffeeId) {
		String insertPromoteFor = "INSERT INTO boutique_coffee.promotefor(promotion_Id, coffee_Id) VALUES(?, ?)";
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement(insertPromoteFor);
			statement.setInt(1, promotionId);
			statement.setInt(2, coffeeId);
			int affectedRows = statement.executeUpdate();

			if(affectedRows == 0) {
				throw new SQLException("Add Promotion For Failed, no rows affected");
			} else {
				return 1;
			}

		} catch (SQLException e) {
			logException(e);
			return -1;
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public int hasPromotion(int storeId, int promotionId) {
		String inserthasPromotion = "INSERT INTO boutique_coffee.haspromotion(store_id, promotion_id) VALUES (?, ?)";
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement(inserthasPromotion);
			statement.setInt(1, storeId);
			statement.setInt(2, promotionId);
			int affectedRows = statement.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("Add hasPromotion failed, no rows affected");
			} else {
				return  1;
			}

		} catch (SQLException e) {
			logException(e);
			return  -1;
		} catch (Exception e) {
			return  -1;
		}
	}

	@Override
	public int addMemberLevel(String name, double boosterFactor) {
		// TODO Jay
		return 0;
	}

	@Override
	public int addCustomer(String firstName, String lastName, String email, int memberLevelId, double totalPoints) {
		// TODO Jay
		return 0;
	}

	@Override
	public int addPurchase(int customerId, int storeId, Date purchaseTime, List<Integer> coffeeIds,
			List<Integer> purchaseQuantities, List<Integer> redeemQuantities) {
		
		beginTransaction(Connection.TRANSACTION_SERIALIZABLE);
		int id = -1;
		String purchaseString = "INSERT INTO boutique_coffee.purchase(customer_id, store_id, purchase_time) VALUES (?, ?, ?)";
		String coffeeString = "INSERT INTO boutique_coffee.buycoffee(purchase_id, coffee_id, purchase_quantity, redeem_quantity) VALUES (?, ?, ?, ?)";
		String fieldName = "purchase_id";
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(purchaseString, new String[] {fieldName});
			stmt.setInt(1, customerId);
			stmt.setInt(2, storeId);
			stmt.setDate(3, purchaseTime);
			int rows = stmt.executeUpdate();
			
			if(rows == 0) {
				throw new SQLException("Add Purchase Failed, no rows affected in purchase table");
			}
			
			ResultSet values = stmt.getGeneratedKeys();
			if(values.next()) {
				id = values.getInt(1);
			}
			
			if (coffeeIds.size() != purchaseQuantities.size() || coffeeIds.size() != purchaseQuantities.size()) {
				throw new SQLException("Add Purchase Failed, array inputs are different sizes");
			}
			
			for(int i = 0; i < coffeeIds.size(); i++) {
				stmt = conn.prepareStatement(coffeeString);
				stmt.setInt(1, id);
				stmt.setInt(2, coffeeIds.get(i));
				stmt.setInt(3, purchaseQuantities.get(i));
				stmt.setInt(4, redeemQuantities.get(i));
				rows = stmt.executeUpdate();
				
				if(rows == 0) {
					throw new SQLException("Add Purchase Failed, no rows affected in buyCoffee table");
				}
			}
			
			
		} catch(SQLException e) {
			logException(e);
			rollback();
			return -1;
			
		} catch(Exception e) {
			rollback();
			return -1;
		}
		
		commit();
		return id;
	}

	@Override
	public List<Integer> getCoffees() {
		LinkedList<Integer> results = new LinkedList<Integer>();
		String queryString = "SELECT coffee_id FROM boutique_coffee.coffee";
		
		try {
			PreparedStatement stmt = conn.prepareStatement(queryString);
			ResultSet values = stmt.executeQuery();
			
			while(values.next()) {
				int id = values.getInt(1);
				results.add(id);
			}
		} catch(SQLException e) {
			logException(e);
			results = new LinkedList<Integer>();
		} catch(Exception e) {
			results = new LinkedList<Integer>();
		}
		
		return results;
	}

	@Override
	public List<Integer> getCoffeesByKeywords(String keyword1, String keyword2) {
		LinkedList<Integer> results = new LinkedList<Integer>();
		String queryString = "SELECT coffee_id FROM boutique_coffee.coffee WHERE name LIKE ? AND name LIKE ?";
		
		try {
			PreparedStatement stmt = conn.prepareStatement(queryString);
			stmt.setString(1, "%" + keyword1 + "%");
			stmt.setString(2, "%" + keyword2 + "%");
			ResultSet values = stmt.executeQuery();
			
			while(values.next()) {
				int id = values.getInt(1);
				results.add(id);
			}
		} catch(SQLException e) {
			logException(e);
			results = new LinkedList<Integer>();
		} catch(Exception e) {
			results = new LinkedList<Integer>();
		}
		
		return results;
	}

	@Override
	public double getPointsByCustomerId(int customerId) {
		double pts;
		String queryString = "SELECT total_points FROM boutique_coffee.customer WHERE customer_id = ?";
		
		try {
			PreparedStatement stmt = conn.prepareStatement(queryString);
			stmt.setInt(1, customerId);
			ResultSet values = stmt.executeQuery();
			
			if(values.next()) {
				pts = values.getDouble(1);
			}
			else {
				pts = -1;
			}
		} catch(SQLException e) {
			logException(e);
			pts = -1;
		} catch(Exception e) {
			pts = -1;
		}
		return pts;
	}

	@Override
	public List<Integer> getTopKStoresInPastXMonth(int k, int x) {
		LinkedList<Integer> results = new LinkedList<Integer>();
		String queryString = "SELECT store_id FROM boutique_coffee.top_stores(?, ?)";
		
		beginTransaction(Connection.TRANSACTION_SERIALIZABLE);
		
		try {
			PreparedStatement stmt = conn.prepareStatement(queryString);
			stmt.setInt(1, k);
			stmt.setInt(2, x);
			ResultSet values = stmt.executeQuery();
			
			while(values.next()) {
				int id = values.getInt(1);
				results.add(id);
			}
		} catch(SQLException e) {
			logException(e);
			rollback();
			return new LinkedList<Integer>();
		} catch(Exception e) {
			rollback();
			return new LinkedList<Integer>();
		}
		
		commit();
		return results;
	}

	@Override
	public List<Integer> getTopKCustomersInPastXMonth(int k, int x) {
		LinkedList<Integer> results = new LinkedList<Integer>();
		String queryString = "SELECT customer_id FROM boutique_coffee.top_customers(?, ?)";
		
		beginTransaction(Connection.TRANSACTION_SERIALIZABLE);
		
		try {
			PreparedStatement stmt = conn.prepareStatement(queryString);
			stmt.setInt(1, k);
			stmt.setInt(2, x);
			ResultSet values = stmt.executeQuery();
			
			while(values.next()) {
				int id = values.getInt(1);
				results.add(id);
			}
		} catch(SQLException e) {
			logException(e);
			rollback();
			return new LinkedList<Integer>();
		} catch(Exception e) {
			rollback();
			return new LinkedList<Integer>();
		}
		
		commit();
		return results;
	}

	private void logException(SQLException e) {
		error_logger.accept("SQL ERROR");
		while(e != null) {
			error_logger.accept(e.getMessage());
			error_logger.accept(e.getSQLState());
			error_logger.accept("" + e.getErrorCode());
			e = e.getNextException();
		}
	}
	
	private void addProcedures(){
		try {
			List<String> lines = Files.readAllLines(Paths.get("jdbc_procedures.sql"));
		    String queryString = String.join("\n", lines.toArray(new String[0]));
		    conn.prepareCall(queryString).execute();
		} catch(SQLException e) {
			logException(e);
		} catch(Exception e) {
			// do nothing
		}
	}
	
	private void beginTransaction() {
		beginTransaction(Connection.TRANSACTION_READ_COMMITTED);
	}
	
	private void beginTransaction(int isolation_level) {
		try {
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(isolation_level);
		} catch (SQLException e) {
			logException(e);
		}
	}
	
	private void commit() {
		try {
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			logException(e);
		}
		
	}
	
	private void rollback() {
		try {
			conn.rollback();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			logException(e);
		}
	}	
}