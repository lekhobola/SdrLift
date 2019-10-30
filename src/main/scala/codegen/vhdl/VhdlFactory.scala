package sdrlift.codegen.vhdl

import sdrlift.model.Actor

import scala.collection.JavaConversions._
import java.util.Arrays.ArrayList

import de.upb.hni.vmagic.VhdlFile
import de.upb.hni.vmagic.libraryunit.Entity
import de.upb.hni.vmagic.`parser`.VhdlParser
import de.upb.hni.vmagic.util.VhdlCollections
import de.upb.hni.vmagic.libraryunit.Architecture
import de.upb.hni.vmagic.`object`.Signal
import de.upb.hni.vmagic.`object`.Constant
import de.upb.hni.vmagic.declaration.SignalDeclaration
import de.upb.hni.vmagic.declaration.Component
import de.upb.hni.vmagic.concurrent.ComponentInstantiation
import de.upb.hni.vmagic.AssociationElement
import de.upb.hni.vmagic.builtin.StdLogic1164
import de.upb.hni.vmagic.parser.antlr.ExpressionType
import de.upb.hni.vmagic.expression.Expression
import de.upb.hni.vmagic.expression.Literal
import de.upb.hni.vmagic.literal.DecimalLiteral
import de.upb.hni.vmagic.literal.BinaryLiteral
import de.upb.hni.vmagic.literal.CharacterLiteral
import de.upb.hni.vmagic.literal.EnumerationLiteral
import de.upb.hni.vmagic.literal.HexLiteral
import de.upb.hni.vmagic.literal.StringLiteral
import de.upb.hni.vmagic.`object`.VhdlObject.Mode
import de.upb.hni.vmagic.builtin.Standard
import de.upb.hni.vmagic.expression.Equals
import de.upb.hni.vmagic.statement.VariableAssignment
import de.upb.hni.vmagic.expression.Aggregate
import de.upb.hni.vmagic.`type`.Type
import de.upb.hni.vmagic.`type`.IndexSubtypeIndication
import de.upb.hni.vmagic.expression.Subtract
import de.upb.hni.vmagic.Range
import de.upb.hni.vmagic.`type`.SubtypeIndication

import scala.collection.JavaConversions._
import scala.util.control.Breaks._
import de.upb.hni.vmagic.literal.PhysicalLiteral



case class VhdlFactory(template: String) {
  /**
    * rename signals in this template (incomplete)
    *
    * @param oldName name of the signal to be renamed
    * @param newName new name of the signal
    * @return VhdlFile containing the new implementation
    * @throws Exception
    */
  @throws(classOf[Exception])
  def renameSignal(oldName: String, newName: String): VhdlFile = {
    // Parse the Template
    val file: VhdlFile = VhdlParser.parseString(template)
    // rename signals in Entity (Port)
    // so get all entities
    val entityList: java.util.List[Entity] = VhdlCollections.getAll(file.getElements, classOf[Entity])
    for (entity <- entityList) {
      // get all ports
      for (sProvider <- entity.getPort) {
        // and finally get signals named oldname
        val s: Signal = VhdlCollections.getByIdentifier(sProvider.getVhdlObjects, classOf[Signal], oldName)
        if (s != null) {
          s.setIdentifier(newName)
        }
      }
    }

    // rename signals in architecture
    val architectureList: java.util.List[Architecture] = VhdlCollections.getAll(file.getElements, classOf[Architecture])
    for (architecture <- architectureList) {

      // rename signals in the declaration
      val declarationList: java.util.List[SignalDeclaration] = VhdlCollections.getAll(architecture.getDeclarations,
        classOf[SignalDeclaration])
      for (declaration <- declarationList) {
        val signalList: java.util.List[Signal] = declaration.getObjects
        for (signal <- signalList) {
          if (signal.getIdentifier.equals(oldName)) {
            signal.setIdentifier(newName)
          }
        }
      }
      // rename signals in implementation
    }

    file
  }

  /**
    * Get all signals defined in this file
    *
    * @param includePort if 'true' signals from port will be included
    * @return A list of signals from the file
    * @throws Exception
    */
  @throws(classOf[Exception])
  def getSignals(includePort: Boolean): java.util.List[Signal] = {
    val signals: java.util.List[Signal] = new java.util.ArrayList[Signal]()
    // Parse the Template
    val file: VhdlFile = VhdlParser.parseString(template)

    // get all entities
    if (includePort) {
      val entityList: java.util.List[Entity] = VhdlCollections.getAll(file.getElements, classOf[Entity])
      for (entity <- entityList) {
        // get all ports
        for (sProvider <- entity.getPort) {
          signals.addAll(VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Signal]))
        }
      }
    }
    signals
  }

  /**
    * Get all generics defined in this file
    *
    * @return A list of generics from the file
    * @throws Exception
    */
  @throws(classOf[Exception])
  def getGenerics(): java.util.List[Constant] = {
    val generics: java.util.List[Constant] = new java.util.ArrayList[Constant]()
    // Parse the Template
    val file: VhdlFile = VhdlParser.parseString(template)

    // get all entities

    val entityList: java.util.List[Entity] = VhdlCollections.getAll(file.getElements, classOf[Entity])
    for (entity <- entityList) {
      // get all generics
      for (sProvider <- entity.getGeneric) {
        generics.addAll(VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Constant]))
      }
    }
    generics
  }

  /**
    * Get the Signal Type for Signal Declaration
    */
  def getWireType(signal: Signal, componentLabel: String, params: java.util.HashMap[String, Any]): SubtypeIndication = {

    if (signal.getType == StdLogic1164.STD_LOGIC) {
      signal.getType
    } else {
      val instance: ComponentInstantiation = new ComponentInstantiation(componentLabel + "Inst", getComponent
      (componentLabel))

      var generics = new java.util.ArrayList[Constant]()
      // get all generics
      for (sProvider <- instance.getComponent.getGeneric) {
        for (c <- VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Constant]))
          generics.add(c)
      }

      // check if a StdLogicVector is of Substrat Type
      if (signal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0).asInstanceOf[Range].getFrom
        .isInstanceOf[Subtract]) {
        // Get the 'From' of the Constant type of StdLogicVector
        val fromIdentifier = signal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0).asInstanceOf[Range]
          .getFrom.asInstanceOf[Subtract].getLeft.asInstanceOf[Constant].getIdentifier

        var genenericDefaultVal: Int = 0

        // get the generic that matches the constant
        breakable {
          for (g <- generics) {
            if (g.getDefaultValue != null) {
              if (g.getIdentifier.equalsIgnoreCase(fromIdentifier)) {
                genenericDefaultVal = g.getDefaultValue.toString.toInt;
                break
              } else genenericDefaultVal = 0
            } else genenericDefaultVal = 0
          }
        }
        // get the ordinal of StdLogicVector
        var ordinal = 0
        if (params != null && !params.isEmpty) {
          if (params.get(fromIdentifier) != None)
            ordinal = params.getOrElse(fromIdentifier, 0).toString.toInt
          else
            ordinal = genenericDefaultVal
        } else ordinal = genenericDefaultVal
        StdLogic1164.STD_LOGIC_VECTOR(ordinal)
      } else {
        StdLogic1164.STD_LOGIC_VECTOR(signal.getType.asInstanceOf[IndexSubtypeIndication].getRanges.get(0)
          .asInstanceOf[Range].getFrom.toString.toInt + 1)
      }
    }
  }

  /**
    * Get component connector signals declaration
    */
  def getComponentWiresDecl(a: Actor, params: java.util.HashMap[String, Any]): List[Signal] = {
    def getSignalMode(s: Signal) = if (s.getMode == Mode.IN) "_i" else "_o"

    val unwantSignals = Set(a.inst + "_clk_i", a.inst + "_rst_i")
    a.signals.map { s =>
      new Signal(a.inst + "_" + s.getIdentifier + getSignalMode(s), getWireType(s, a.id, params), if (s.getType ==
        StdLogic1164.STD_LOGIC) StdLogic1164.STD_LOGIC_0 else Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
    }.toList.filterNot(x => unwantSignals.contains(x.getIdentifier))
  }

  /**
    * Get component connector signal that matches a specified string
    */
  def getComponentWire(signal: String, a: Actor, labels: java.util.HashMap[String, String], label: String): Signal = {

    if (signal.equalsIgnoreCase("en"))
      new Signal(getPortLabel("en", labels, label), StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("din")) new Signal(getPortLabel("din", labels, label),
      StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("vld")) new Signal(getPortLabel("vld", labels, label),
      StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("dout")) new Signal(getPortLabel("dout", labels, label),
      StdLogic1164.STD_LOGIC_VECTOR(0))
    else null

  }

  /**
    * Get fifo channel target consumption access pattern declaration
    */
  def getChannelCpDecl(label: String, constVal: String): Constant = {

    new Constant(label + "_cp", StdLogic1164.STD_LOGIC_VECTOR(constVal.length), new BinaryLiteral(constVal.reverse))
  }

  /**
    * Get fifo channel target consumption access pattern declaration
    */
  def getChannelCp(label: String): Constant = {

    new Constant(label + "_cp", StdLogic1164.STD_LOGIC_VECTOR(0))
  }

  /**
    * Get fifo channel connector signals declaration
    */
  def getChannelWiresDecl(signals: java.util.List[Signal], label: String, dWidth: Int): List[Signal] = {
    val l1 = signals.map {
      s =>
        val str = s.getIdentifier.toString

        if (str.equalsIgnoreCase("we")) new Signal(label + "_we", s.getType, StdLogic1164.STD_LOGIC_0)
        else if (str.equalsIgnoreCase("re")) new Signal(label + "_re", s.getType, StdLogic1164.STD_LOGIC_0)
        else if (str.equalsIgnoreCase("em")) new Signal(label + "_em", s.getType, StdLogic1164.STD_LOGIC_0);
        else if (str.equalsIgnoreCase("din")) new Signal(label + "_din", StdLogic1164.STD_LOGIC_VECTOR(dWidth),
          Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
        else if (str.equalsIgnoreCase("dout")) new Signal(label + "_dout", StdLogic1164.STD_LOGIC_VECTOR(dWidth),
          Aggregate.OTHERS(StdLogic1164.STD_LOGIC_0))
        else if (str.equalsIgnoreCase("fl")) new Signal(label + "_fl", s.getType, StdLogic1164.STD_LOGIC_0)
        else if (str.equalsIgnoreCase("vld")) new Signal(label + "_vld", s.getType, StdLogic1164.STD_LOGIC_0)
        else null

    }.toList.filter(_ != null)

    l1
  }

  /**
    * Get the port signal label
    */
  def getPortLabel(signal: String, labels: java.util.HashMap[String, String], label: String): String = {
    val l = labels.find(_._1.equalsIgnoreCase(label))
    val m = labels.find(_._2.equalsIgnoreCase(signal))
    if (l != None)
      l.get._1
    else if (m != None) //if(label.equalsIgnoreCase(signal))
      m.get._1
    else {
      "open"
    }
  }


  /**
    * Get fifo channel connector signal that matches a specified string
    */
  def getChannelWire(signal: String, label: String): Signal = {
    if (signal.equalsIgnoreCase("we")) new Signal(label + "_we", StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("re")) new Signal(label + "_re", StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("em")) new Signal(label + "_em", StdLogic1164.STD_LOGIC_VECTOR(0));
    else if (signal.equalsIgnoreCase("din")) new Signal(label + "_din", StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("dout")) new Signal(label + "_dout", StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("fl")) new Signal(label + "_fl", StdLogic1164.STD_LOGIC_VECTOR(0))
    else if (signal.equalsIgnoreCase("vld")) new Signal(label + "_vld", StdLogic1164.STD_LOGIC_VECTOR(0))
    else null
  }

  @throws(classOf[Exception])
  def getEntity(identifier: String): Entity = {
    // Parse the Template
    val file: VhdlFile = VhdlParser.parseString(template)
    val entity: Entity = VhdlCollections.getByIdentifier(file.getElements, classOf[Entity], identifier)
    entity
  }

  @throws(classOf[Exception])
  def getComponent(identifier: String): Component = {
    new Component(getEntity(identifier))
  }

  @throws(classOf[Exception])
  def getComponentInstantiationV1(identifier: String, incomplete: Boolean): ComponentInstantiation = {

    val instance: ComponentInstantiation = new ComponentInstantiation("myInstance", getComponent(identifier))
    // get all ports
    for (sProvider <- instance.getComponent.getPort) {
      val signals: java.util.List[Signal] = VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Signal])
      for (s <- signals) {
        instance.getPortMap.add(new AssociationElement(s.getIdentifier, s))
      }
      if (incomplete) {
        // remove first element
        instance.getPortMap.remove(0)
      }
    }
    return instance

  }

  @throws(classOf[Exception])
  def getComponentInstantiation(aIdentifier: String, aInstanceName: String, aMappedGenerics: java.util
  .HashMap[String, Any], aMappedSignals: List[Signal]): ComponentInstantiation = {
    def isAllDigits(x: String) = x forall Character.isDigit
    // generic type
    def hdlLiteralVal(aParam: Any) = {
      val param = aParam
      aParam match {
        case d: Integer => new DecimalLiteral(param.asInstanceOf[Int])
        case c: Character => new CharacterLiteral(param.asInstanceOf[Character])
        case s: String => {
          val strParam = param.asInstanceOf[String]
          val physicalArr = ("""\(.*?\)""".r findAllIn strParam).toList
          if (!physicalArr.isEmpty) {
            new PhysicalLiteral(physicalArr.head)
          }
          else if (strParam.substring(0, 1).equalsIgnoreCase("b"))
            new BinaryLiteral(strParam.substring(1, strParam.length))
          else if (strParam.substring(0, 1).equalsIgnoreCase("c"))
            new CharacterLiteral(strParam.charAt(1))
          else if (strParam.substring(0, 1).equalsIgnoreCase("x"))
            new HexLiteral(strParam.substring(1, strParam.length))
          else if (isAllDigits(strParam)) new DecimalLiteral(strParam.toInt)
          else new StringLiteral(strParam)
        }
        // case "class java.lang.Boolean"    =>  new EnumerationLiteral(param.asInstanceOf[Boolean])
        case _ => new BinaryLiteral(param.asInstanceOf[String])
      }
    }

    // component instantiation
    val instance: ComponentInstantiation = new ComponentInstantiation(aInstanceName, getComponent(aIdentifier))
    // retrieve all generic parameters
    for (sProvider <- instance.getComponent.getGeneric) {
      val generics: java.util.List[Constant] = VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Constant])
      for (generic <- generics) {
        val param =
          if (aMappedGenerics == null || aMappedGenerics.isEmpty) null
          else aMappedGenerics.getOrElse(generic.getIdentifier, null)
        val mappedGeneric = if (param != null) {
          generic.setDefaultValue(hdlLiteralVal(param))
        } else if (generic.getDefaultValue != null) {
          generic.setDefaultValue(generic.getDefaultValue)
        } else {
          generic.setDefaultValue(null)
        }

        instance.getGenericMap.add(new AssociationElement(generic.getIdentifier, generic.getDefaultValue))
      }
    }

    // retrieve all signal
    for (sProvider <- instance.getComponent.getPort) {
      val signals: java.util.List[Signal] = VhdlCollections.getAll(sProvider.getVhdlObjects, classOf[Signal])
      for (signal <- signals) {
        val mappedSignal = if (signal.getIdentifier.equalsIgnoreCase("clk") || signal.getIdentifier.equalsIgnoreCase
        ("rst")) signal
        else {
          val index = aMappedSignals.indexWhere { x =>
            x.getIdentifier.contains(aInstanceName + "_" + signal
              .getIdentifier)
          }
          if (index > -1)
            aMappedSignals(index)
          else
            new Signal("unknown", signal.getType)
        }
        instance.getPortMap.add(new AssociationElement(signal.getIdentifier, mappedSignal))
      }
    }
    return instance
  }

}
