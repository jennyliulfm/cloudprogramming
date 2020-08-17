package edu.utas.dao;

import edu.utas.util.Consts;
import edu.utas.util.DBAdaptor;
import edu.utas.vo.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class JobDAO {
    public void closeConnection(Connection con)
    {
        try
        {
            if(con!=null)
            {
                con.close();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Get Jobs by their status from DB
    public ArrayList<Job> getJobsByStatus(String sts) {

        ArrayList<Job> jobs = new ArrayList<Job>();
        Connection dbConn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            String query = "SELECT * FROM job WHERE status= ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, sts);
            rs = statement.executeQuery();

            while (rs.next()) {
                Integer jobID = rs.getInt("id");
                String passcode = rs.getString("passcode");
                String filename = rs.getString("filename");
                String input = rs.getString("input");
                if(input == null)
                {
                    input = "";
                }

                Job jb = new Job();
                jb.setId(jobID);
                jb.setPasscode(passcode);
                jb.setFilename(filename);
                jb.setStatus(Consts.JOB_INIT);
                jb.setWorker(rs.getString("worker"));
                jb.setCreated(rs.getDate("created"));
                jb.setDuration(rs.getDouble("duration"));
                jb.setExpectation(rs.getInt("expectation"));
                jb.setInput(input);

                jobs.add(jb);

                System.out.println("ID is:" +jb.getId());
                System.out.println("Passcode is: " + jb.getPasscode());
                System.out.println("Filename is:" + jb.getFilename());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }

            closeConnection(dbConn);
        }
        return jobs;
    }

    //Get Jobs by their status from DB
    public String getJobStatusByPasscode(String sts) {

        ArrayList<Job> jobs = new ArrayList<Job>();
        Connection dbConn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        String result = null;

        try {
            String query = "SELECT status FROM job WHERE passcode= ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, sts);
            rs = statement.executeQuery();

            while (rs.next()) {
                result = rs.getString("status");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }

            closeConnection(dbConn);
        }
        return result;
    }

    public void updateJobStatusByID(int jbid, String s,String workerID)
    {
        Connection dbConn = null;
        PreparedStatement statement = null;
        try {

            String query = "UPDATE job set status = ?,worker = ? where id = ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, s);
            statement.setString(2,workerID);
            statement.setInt(3, jbid);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
            closeConnection(dbConn);
        }
    }

    public void updateJobByCode(String code,Double d, String s)
    {
        Connection dbConn = null;
        PreparedStatement statement = null;

        try {

            String query = "UPDATE job set status = ?, duration = ? where passcode = ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, s);
            statement.setDouble(2, d);
            statement.setString(3,code);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();

        }
        finally {
            if(statement!=null)
            {
                try
                {
                    statement.close();
                }catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
            closeConnection(dbConn);
        }
    }


    public void updateStatusByCode(String code, String s)
    {
        Connection dbConn = null;
        PreparedStatement statement = null;
        try {

            String query = "UPDATE job set status = ? where passcode = ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, s);
            statement.setString(2, code);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
            closeConnection(dbConn);
        }

    }

    public void updateFinishedJobsByCode(String code, String s, Double d, Double c, String result)
    {
        Connection dbConn = null;
        PreparedStatement statement = null;
        try {

            //dbConn.setAutoCommit(false);
            String query = "UPDATE job set status = ?, duration =?, cost =?, result =? where passcode = ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, s);
            statement.setDouble(2, d);
            statement.setDouble(3, c);

            statement.setString(4, result);
            statement.setString(5, code);
            statement.executeUpdate();
            //dbConn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
            closeConnection(dbConn);
        }

    }

    public ArrayList<Job> getMigratedJobs(String workerid, String s)
    {
        ArrayList<Job> jobs = new ArrayList<Job>();
        Connection dbConn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            String query = "SELECT * FROM job WHERE worker=? and status =?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, workerid);
            statement.setString(2, s);
            rs = statement.executeQuery();

            while (rs.next()) {
                Integer jobID = rs.getInt("id");
                String passcode = rs.getString("passcode");
                String filename = rs.getString("filename");
                String input = rs.getString("input");
                if(input==null)
                {
                    input = "";
                }

                Job jb = new Job();
                jb.setId(jobID);
                jb.setPasscode(passcode);
                jb.setFilename(filename);
                jb.setStatus(Consts.JOB_INIT);
                jb.setCreated(rs.getDate("created"));
                jb.setExpectation(rs.getInt("expectation"));
                jb.setInput(input);

                jobs.add(jb);

                System.out.println("ID is:" +jb.getId());
                System.out.println("Passcode is: " + jb.getPasscode());
                System.out.println("Filename is:" + jb.getFilename());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }

            closeConnection(dbConn);
        }
        return jobs;
    }

    public void updateJobStatusByID(int jobid, String s)
    {
        Connection dbConn = null;
        PreparedStatement statement = null;
        try {

            String query = "UPDATE job set status = ? where id = ?;";
            dbConn = DBAdaptor.getConnection();
            statement = dbConn.prepareStatement(query);
            statement.setString(1, s);
            statement.setInt(2, jobid);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (statement!=null)
            {
                try
                {
                    statement.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
            closeConnection(dbConn);
        }

    }
}
