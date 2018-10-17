package v2.models.inbound

import play.api.mvc.AnyContent

case class EopsDeclarationInputData(nino: String, from: String, to: String, body: AnyContent) extends InputData
