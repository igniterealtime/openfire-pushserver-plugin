package org.igniterealtime.openfire.plugins.pushserver

import org.jivesoftware.database.DbConnectionManager
import org.jivesoftware.util.JiveGlobals
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement

object DbUtils {

    private val logger = LoggerFactory.getLogger(DbUtils::class.java)

    private fun createConnection(): Connection? {
        return try {
            val conn = DbConnectionManager.getConnection()
            conn.autoCommit = false
            conn
        } catch (e: Exception) {
            return null
        }
    }

    fun createStatement(connection: Connection, sql: String, parameterList: List<Any>? = null): PreparedStatement? {
        return try {
            val statement = connection.prepareStatement(sql)
            parameterList?.forEachIndexed { index, element ->
                when(element) {
                    is String   -> statement.setString(index+1, element)
                    is Int      -> statement.setInt(index+1, element)
                    is Boolean  -> statement.setBoolean(index+1, element)
                    else        -> statement.setObject(index+1, element)
                }
            }
            statement
        } catch (e: Exception) {
            logger.error("Statement could not been created.", e)
            DbConnectionManager.closeConnection(connection)
            return null
        }
    }

    fun <T> doWithConnection(sql: String, parameterList: List<Any>? = null, closure: (Connection, PreparedStatement) -> T, onFailed: (e: Exception) -> Unit): T? {
        val connection = createConnection() ?: return null
        val statement = createStatement(connection, sql, parameterList) ?: return null

        return try {
            val e = closure(connection, statement)
            connection.commit()
            e
        } catch (e: Exception) {
            onFailed(e)
            null
        } finally {
            DbConnectionManager.closeConnection(statement, connection)
        }
    }

}