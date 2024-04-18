import com.typesafe.config.ConfigFactory
import li.cil.oc.Settings
import li.cil.oc.server.component.InternetCard
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.wordspec.AnyWordSpec

import java.net.InetAddress
import java.lang.System
import scala.io.{Codec, Source}

class InternetFilteringRuleTest extends AnyFunSpec {
  val config = autoClose(classOf[Settings].getResourceAsStream("/application.conf")) { in =>
    val configStr = Source.fromInputStream(in)(Codec.UTF8).getLines().mkString("", System.lineSeparator(), System.lineSeparator())
    ConfigFactory.parseString(configStr)
  }
  val settings = new Settings(config.getConfig("opencomputers"))


  describe("The default AddressValidators") {
    // Many of these payloads are pulled from PayloadsAllTheThings
    // https://github.com/swisskyrepo/PayloadsAllTheThings/blob/master/Server%20Side%20Request%20Forgery/README.md
    it("should accept a valid external address") {
      assert(!isUriBlacklisted("https://google.com"))
    }
    it("should reject localhost") {
      assert(isUriBlacklisted("http://localhost"))
    }
    it("should reject the local host in IPv4 format") {
      assert(isUriBlacklisted("http://127.0.0.1"))
      assert(isUriBlacklisted("http://127.0.1"))
      assert(isUriBlacklisted("http://127.1"))
      assert(isUriBlacklisted("http://0"))
    }
    it("should reject the local host in IPv6") {
      assert(isUriBlacklisted("http://[::1]"))
      assert(isUriBlacklisted("http://[::]"))
    }
    it("should reject IPv6/IPv4 Address Embedding") {
      assert(isUriBlacklisted("http://[0:0:0:0:0:ffff:127.0.0.1]"))
      assert(isUriBlacklisted("http://[::ffff:127.0.0.1]"))
    }
    it("should reject an attempt to bypass using a decimal IP location") {
      assert(isUriBlacklisted("http://2130706433")) // 127.0.0.1
      assert(isUriBlacklisted("http://3232235521")) // 192.168.0.1
      assert(isUriBlacklisted("http://3232235777")) // 192.168.1.1
    }
    it("should reject the IMDS address in IPv4 format") {
      assert(isUriBlacklisted("http://169.254.169.254"))
      assert(isUriBlacklisted("http://2852039166")) // 169.254.169.254
    }
    it("should reject the IMDS address in IPv6 format") {
      assert(isUriBlacklisted("http://[fd00:ec2::254]"))
    }
    it("should reject the IMDS in for Oracle Cloud") {
      assert(isUriBlacklisted("http://192.0.0.192"))
    }
    it("should reject the IMDS in for Alibaba Cloud") {
      assert(isUriBlacklisted("http://100.100.100.200"))
    }
  }

  def isUriBlacklisted(uri: String): Boolean = {
    val uriObj = new java.net.URI(uri)
    val resolved = InetAddress.getByName(uriObj.getHost)
    !InternetCard.isRequestAllowed(settings, resolved, uriObj.getHost)
  }

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: (A) ⇒ B): B = {
    var t: Throwable = null
    try {
      fun(closeable)
    } catch {
      case funT: Throwable ⇒
        t = funT
        throw t
    } finally {
      if (t != null) {
        try {
          closeable.close()
        } catch {
          case closeT: Throwable ⇒
            t.addSuppressed(closeT)
            throw t
        }
      } else {
        closeable.close()
      }
    }
  }

}
