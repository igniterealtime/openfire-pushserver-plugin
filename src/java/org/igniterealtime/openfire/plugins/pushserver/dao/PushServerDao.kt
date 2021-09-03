package org.igniterealtime.openfire.plugins.pushserver.dao

import org.igniterealtime.openfire.plugins.pushserver.DbUtils
import org.igniterealtime.openfire.plugins.pushserver.models.PushRecord
import org.slf4j.LoggerFactory

object PushServerDao {

    private val logger = LoggerFactory.getLogger(PushServerDao::class.java)

    private const val TABLE_NAME = "ofPushServer"

    private const val ADD_PUSH_RECORD = """
        INSERT INTO $TABLE_NAME (domain, deviceId, token, node, secret, type) 
            VALUES (?, ?, ?, ?, ?, ?) 
            ON DUPLICATE KEY UPDATE token = VALUES(token)
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

    fun addPushRecord(pushRecord: PushRecord): PushRecord? {
        return DbUtils.doWithConnection(
            ADD_PUSH_RECORD
            , listOf(pushRecord.domain, pushRecord.deviceId, pushRecord.token, pushRecord.node, pushRecord.secret, pushRecord.type.name)
            , { conn, statement ->
                statement.executeUpdate()

                val rs = DbUtils.createStatement(
                    conn
                    , SELECT_PUSH_RECORD
                    , listOf(pushRecord.domain, pushRecord.deviceId)
                )?.executeQuery()

                if (rs?.next() == true) {
                    PushRecord(
                        domain = pushRecord.domain
                        , deviceId = pushRecord.deviceId
                        , token = rs.getString("token")
                        , type = rs.getString("type")
                        , node = rs.getString("node")
                        , secret = rs.getString("secret")
                    )
                } else {
                    null
                }
            }
        ) {
            logger.error("PushRecord couldn't be inserted.", it)
        }
    }

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