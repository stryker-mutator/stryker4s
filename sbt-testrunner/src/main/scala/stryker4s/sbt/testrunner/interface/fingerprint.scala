package stryker4s.sbt.testrunner.interface

final case class AnnotatedFingerprintImpl(isModule: Boolean, annotationName: String)
    extends sbt.testing.AnnotatedFingerprint

final case class SubclassFingerprintImpl(isModule: Boolean, superclassName: String, requireNoArgConstructor: Boolean)
    extends sbt.testing.SubclassFingerprint
