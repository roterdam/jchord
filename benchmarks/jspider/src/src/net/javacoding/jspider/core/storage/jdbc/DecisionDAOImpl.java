package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.storage.DecisionDAO;
import net.javacoding.jspider.core.storage.spi.DecisionDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.api.model.*;

import java.sql.*;

/**
 * $Id: DecisionDAOImpl.java,v 1.3 2003/04/11 16:37:06 vanrogu Exp $
 */
class DecisionDAOImpl implements DecisionDAOSPI {

    public static final int SUBJECT_SPIDER = 1;
    public static final int SUBJECT_PARSE = 2;

    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_SUBJECT = "subject";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_COMMENT = "comment";
    public static final String ATTRIBUTE_DECISION = "decision";
    public static final String ATTRIBUTE_RULE = "rule";

    protected Log log;

    protected DBUtil dbUtil;
    protected StorageSPI storage;

    public DecisionDAOImpl ( StorageSPI storage, DBUtil dbUtil ) {
        this.log = LogFactory.getLog(DecisionDAO.class);
        this.dbUtil = dbUtil;
        this.storage = storage;
    }

    public void saveSpiderDecision(ResourceInternal resource, DecisionInternal decision) {
        saveDecision(SUBJECT_SPIDER,resource, decision);
    }

    public void saveParseDecision(ResourceInternal resource, DecisionInternal decision) {
        saveDecision(SUBJECT_PARSE,resource, decision);
    }

    protected void saveDecision ( int subject, ResourceInternal resource, DecisionInternal decision ) {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            Connection connection = dbUtil.getConnection();
            ps = connection.prepareStatement("insert into jspider_decision ( resource, subject, type, comment ) values (?,?,?,?)");
            ps.setInt(1, resource.getId());
            ps.setInt(2, (subject));
            ps.setInt(3, (decision.getDecision()));
            ps.setString(4, (decision.getComment()));
            ps.executeUpdate();

            DecisionStep[] steps = decision.getSteps();
            for (int i = 0; i < steps.length; i++) {
                DecisionStep step = steps[i];
                ps2 = connection.prepareStatement("insert into jspider_decision_step ( resource, subject, sequence, type, rule, decision, comment ) values (?,?,?,?,?,?,?)");
                ps2.setInt(1, resource.getId());
                ps2.setInt(2, subject);
                ps2.setInt(3, i);
                ps2.setInt(4, (step.getRuleType())) ;
                ps2.setString(5, (step.getRule()));
                ps2.setInt(6, (step.getDecision()));
                ps2.setString(7, (step.getComment()));
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally{
            dbUtil.safeClose(ps, log);
            dbUtil.safeClose(ps2, log);
        }
    }

    public DecisionInternal findSpiderDecision(ResourceInternal resource) {
        return findDecision ( SUBJECT_SPIDER, resource);
    }

    public DecisionInternal findParseDecision(ResourceInternal resource) {
        return findDecision ( SUBJECT_PARSE, resource);
    }

    protected DecisionInternal findDecision ( int subject, ResourceInternal resource ) {
        DecisionInternal decision = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        try {
            Connection connection = dbUtil.getConnection();
            ps = connection.prepareStatement("select * from jspider_decision where resource=? and subject=?");
            ps.setInt(1, resource.getId());
            ps.setInt(2, subject);
            rs = ps.executeQuery();
            if ( rs.next() ) {
                int type = rs.getInt(ATTRIBUTE_TYPE);
                String comment = rs.getString(ATTRIBUTE_COMMENT);
                decision = new DecisionInternal ( type, comment );

                ps2 = connection.prepareStatement("select * from jspider_decision_step where resource=? and subject=? order by sequence");
                ps2.setInt(1, resource.getId());
                ps2.setInt(2, subject);
                rs2 = ps2.executeQuery();
                while ( rs2.next ( ) ) {
                    String rule = rs2.getString(ATTRIBUTE_RULE);
                    int stepType = rs2.getInt(ATTRIBUTE_TYPE);
                    int stepDecision = rs2.getInt(ATTRIBUTE_DECISION);
                    String stepComment = rs2.getString(ATTRIBUTE_COMMENT);
                    decision.addStep(rule,stepType, stepDecision, stepComment);
                }
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally{
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(ps, log);
            dbUtil.safeClose(rs2, log);
            dbUtil.safeClose(ps2, log);
        }
        return decision;
    }

}
