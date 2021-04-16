package astah.plugin

import com.change_vision.jude.api.inf.model.*
import javax.swing.JButton

class ButtonMakeInstanceOfRelation: JButton("Make Instance-Of Relation") {
    fun push(diagram: IDiagram?) {
        val selectedElements = InstanceOf.isSelectedElementsInClassOrBlockDiagram(diagram) ?: return
        val selectedAssociations = selectedElements.filterIsInstance<IAssociation>()
        if (selectedAssociations.isEmpty()) {
            println("Select any relation")
            return
        }
        selectedAssociations.forEach { association ->
            AstahAccessor.addStereotype(association, "instance-of")
        }
    }
}