package li.cil.oc.server

import scala.language.implicitConversions

package object component {
  implicit def result(args: Any*): Array[AnyRef] = li.cil.oc.util.ResultWrapper.result(args*)
}
