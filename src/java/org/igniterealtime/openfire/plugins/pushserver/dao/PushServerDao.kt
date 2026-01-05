package org.igniterealtime.openfire.plugins.pushserver.dao

import org.igniterealtime.openfire.plugins.pushserver.DbUtils
import org.igniterealtime.openfire.plugins.pushserver.models.PushRecord
import org.slf4j.LoggerFactory

object PushServerDao {

    private val logger = LoggerFactory.getLogger(PushServerDao::class.java)

    private const val TABLE_NAME = "ofPushServer"

    private const val PUSH_RECORD_COUNT = """
        SELECT COUNT(1)
            FROM $TABLE_NAME
        WHERE domain = ?
            AND deviceId = ?
    """
    private const val ADD_PUSH_RECORD = """
        INSERT INTO $TABLE_NAME (domain, deviceId, token, node, secret, type) 
            VALUES (?, ?, ?, ?, ?, ?);
    """
    private const val UPDATE_PUSH_RECORD = """
        UPDATE $TABLE_NAME SET token = ? 
        WHERE domain = ? AND deviceId = ?
    """
    private const val UPSERT_PUSH_RECORD = """
        INSERT INTO $TABLE_NAME (domain, deviceId, token, node, secret, type) 
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (domain, deviceId) DO UPDATE SET token=?  
    """
    private const val UPSERT_PUSH_RECORD_SQL = """
        INSERT INTO $TABLE_NAME (domain, deviceId, token, node, secret, type) 
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE token = VALUES(token);
    """
    private const val DELETE_PUSH_RECORD = """
        DELETE FROM $TABLE_NAME 
        WHERE domain = ? AND deviceId = ?
    """
    private const val SELECT_PUSH_RECORD = """
        SELECT token, node, secret, type 
        FROM $TABLE_NAME 
        WHERE domain = ? AND deviceId = ?
    """
    private const val SELECT_PUSH_RECORD_WITH_NODE = """
        SELECT deviceId, token, secret, type 
        FROM $TABLE_NAME 
        WHERE domain = ? AND node = ?
    """

fun addPushRecord(pushRecord: PushRecord): PushRecord? {  //select based
    try {
        val count = DbUtils.doWithConnection(
            sql = PUSH_RECORD_COUNT,
            parameterList = listOf(pushRecord.domain, pushRecord.deviceId),
            closure = { _, stmt ->
                val rs = stmt.executeQuery()
                val count = if (rs.next()) rs.getInt(1) else 0
                rs.close()
                count
            },
            onFailed = {
                logger.error("Failed to check existing push record count.", it)
            }
        ) ?: 0

        if (count > 0) {
            DbUtils.doWithConnection(
                sql = UPDATE_PUSH_RECORD,
                parameterList = listOf(
                    pushRecord.token,
                    pushRecord.domain,
                    pushRecord.deviceId
                ),
                closure = { _, stmt ->
                    stmt.executeUpdate()
                },
                onFailed = {
                    logger.error("Failed to update push record.", it)
                }
            )
        } else {
            DbUtils.doWithConnection(
                sql = ADD_PUSH_RECORD,
                parameterList = listOf(
                    pushRecord.domain,
                    pushRecord.deviceId,
                    pushRecord.token,
                    pushRecord.node,
                    pushRecord.secret,
                    pushRecord.type.name
                ),
                closure = { _, stmt ->
                    stmt.executeUpdate()
                },
                onFailed = {
                    logger.error("Failed to insert push record.", it)
                }
            )
        }

        return DbUtils.doWithConnection(
            sql = SELECT_PUSH_RECORD,
            parameterList = listOf(pushRecord.domain, pushRecord.deviceId),
            closure = { _, stmt ->
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    PushRecord(
                        domain = pushRecord.domain,
                        deviceId = pushRecord.deviceId,
                        token = rs.getString("token"),
                        node = rs.getString("node"),
                        secret = rs.getString("secret"),
                        type = rs.getString("type")
                    )
                } else null
            },
            onFailed = {
                logger.error("Failed to fetch push record after insert/update.", it)
            }
        )
    } catch (e: Exception) {
        logger.error("Unexpected failure in addPushRecord()", e)
        return null
    }
}

    // fun addPushRecord(pushRecord: PushRecord): PushRecord? {        // try catch based
    //     return DbUtils.doWithConnection(
    //         UPSERT_PUSH_RECORD
    //         , listOf(pushRecord.domain, pushRecord.deviceId, pushRecord.token, pushRecord.node, pushRecord.secret, pushRecord.type.name, pushRecord.token)
    //         , { conn, statement ->
    //           try {
    //             statement.executeUpdate()
    //         } catch (e: Exception) {
    //             val insertStatement = DbUtils.createStatement(
    //                 conn,
    //                 UPSERT_PUSH_RECORD_SQL,
    //                 listOf(
    //                     pushRecord.domain,
    //                     pushRecord.deviceId,
    //                     pushRecord.token,
    //                     pushRecord.node,
    //                     pushRecord.secret,
    //                     pushRecord.type.name
    //                 )
    //             )
    //             insertStatement?.executeUpdate()
    //         }

    //             val rs = DbUtils.createStatement(
    //                 conn
    //                 , SELECT_PUSH_RECORD
    //                 , listOf(pushRecord.domain, pushRecord.deviceId)
    //             )?.executeQuery()

    //             if (rs?.next() == true) {
    //                 PushRecord(
    //                     domain = pushRecord.domain
    //                     , deviceId = pushRecord.deviceId
    //                     , token = rs.getString("token")
    //                     , type = rs.getString("type")
    //                     , node = rs.getString("node")
    //                     , secret = rs.getString("secret")
    //                 )
    //             } else {
    //                 null
    //             }
    //         }
    //     ) {
                
    //         logger.error("PushRecord couldn't be inserted.", it)
    //     }
    // }

    // fun addPushRecordOld(pushRecord: PushRecord): PushRecord? {
    //     return DbUtils.doWithConnection(
    //         ADD_PUSH_RECORD
    //         , listOf(pushRecord.domain, pushRecord.deviceId, pushRecord.token, pushRecord.node, pushRecord.secret, pushRecord.type.name, pushRecord.token)
    //         , { conn, statement ->
    //             statement.executeUpdate()

    //             val rs = DbUtils.createStatement(
    //                 conn
    //                 , SELECT_PUSH_RECORD
    //                 , listOf(pushRecord.domain, pushRecord.deviceId)
    //             )?.executeQuery()

    //             if (rs?.next() == true) {
    //                 PushRecord(
    //                     domain = pushRecord.domain
    //                     , deviceId = pushRecord.deviceId
    //                     , token = rs.getString("token")
    //                     , type = rs.getString("type")
    //                     , node = rs.getString("node")
    //                     , secret = rs.getString("secret")
    //                 )
    //             } else {
    //                 null
    //             }
    //         }
    //     ) {
    //         logger.error("PushRecord couldn't be inserted.", it)
    //     }
    // }

    fun deletePushRecord(domain: String, deviceId: String): Boolean? {
        return DbUtils.doWithConnection(
            DELETE_PUSH_RECORD
            , listOf(domain, deviceId)
            , { _, statement ->
                statement.executeUpdate()
                true
            }
        ) {
            logger.error("PushRecord couldn't be deleted.", it)
        }
    }

    fun getPushRecord(domain: String, node: String): PushRecord? {
        return DbUtils.doWithConnection(
            SELECT_PUSH_RECORD_WITH_NODE
            , listOf(domain, node)
            , { _, statement ->
                val rs = statement.executeQuery()
                if (rs.next()) {
                    PushRecord(
                        domain = domain
                        , deviceId = rs.getString("deviceId")
                        , token = rs.getString("token")
                        , type = rs.getString("type")
                        , node = node
                        , secret = rs.getString("secret")
                    )
                } else {
                    null
                }
            }
        ) {
            logger.error("PushRecord couldn't be fetched.", it)
        }
    }

}