package net.devslash.post

import net.devslash.FullDataPostHook
import net.devslash.HttpRequest
import net.devslash.HttpResponse
import net.devslash.RequestData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction


open class BaseMySQLSettings {
  var url = ""
  var username = ""
  var password = ""
}

class MySqlSettingsBuilder<T : Table> : BaseMySQLSettings() {
  var table: T? = null
  var insertStatement: ((HttpRequest, HttpResponse, RequestData) -> (T.(InsertStatement<Number>) -> Unit))? =
      null

  fun build(): MySQLSettings<T> {
    return MySQLSettings(url, username, password, table!!, insertStatement!!)
  }
}

data class MySQLSettings<T : Table>(var url: String,
                                    var username: String,
                                    var password: String,
                                    var table: T,
                                    val insertStatement: (HttpRequest, HttpResponse, RequestData) -> (T.(InsertStatement<Number>) -> Unit))

class StoreMySQL<T : Table>(res: MySqlSettingsBuilder<T>.() -> Unit) : FullDataPostHook {

  private val settings = MySqlSettingsBuilder<T>().apply(res).build()

  init {
    Database.connect(settings.url, "com.mysql.jdbc.Driver", settings.username, settings.password)
  }

  override fun accept(req: HttpRequest, resp: HttpResponse, data: RequestData) {
    val table: T = settings.table

    val insert: (T, InsertStatement<Number>) -> Unit =
        settings.insertStatement.invoke(req, resp, data)
    transaction {
      table.insert(insert)
    }
  }
}
