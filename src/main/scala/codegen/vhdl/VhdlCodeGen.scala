package sdrlift.codegen.vhdl

import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.libraryunit.{Architecture, Entity}


abstract class VhdlCodeGen {
  /**
    * creates the VHDL file
    *
    * @return A VhdlFile containing the implementation
    */
   def getHdlFile(libClauses: List[String] = null, useClauses: List[String] = null): VhdlFile

  /**
    * Create the entity
    *
    * @return the entity
    */
   def getHdlEntity: Entity

  /**
    * Implement the template, declare internal signals etc.
    *
    * @return the architecture
    */
   def getHdlArchitecture: Architecture
}