package org.zenboot.portal

import org.springframework.dao.DataIntegrityViolationException
import org.zenboot.portal.processing.ExecutionZone
import org.springframework.http.HttpStatus

class HostController extends AbstractRestController {

    static allowedMethods = [update: "POST", delete: "POST"]

    def rest = {
        Host host = Host.findById(params.id)

        if (!host) {
            this.renderRestResult(HttpStatus.NOT_FOUND)
            return
        }
        this.renderRestResult(HttpStatus.OK, host)
        return
    }


    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)

        def hostInstanceList
        def hostInstanceTotal
        def parameters = [:]
        if (params.execId) {
          def executionZoneInstance = ExecutionZone.get(params.execId)
          if (!executionZoneInstance) {
              flash.message = message(code: 'default.not.found.message', args: [message(code: 'executionZone.label', default: 'ExecutionZone'), params.execId])
              redirect(action: "list")
              return
          }
            parameters.execId = params.execId
            hostInstanceList = Host.findAllByExecZone(executionZoneInstance, params)
            hostInstanceTotal = Host.findAllByExecZone(executionZoneInstance).size()
        } else {
            hostInstanceList = Host.list(params)
            hostInstanceTotal = Host.count()
        }


        [hostInstanceList: hostInstanceList, hostInstanceTotal: hostInstanceTotal, parameters:parameters]
    }

    def show() {
        def hostInstance = Host.get(params.id)
        if (!hostInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'host.label', default: 'Host'), params.id])
            redirect(action: "list")
            return
        }

        [hostInstance: hostInstance]
    }

    def edit() {
        def hostInstance = Host.get(params.id)
        if (!hostInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'host.label', default: 'Host'), params.id])
            redirect(action: "list")
            return
        }

        [hostInstance: hostInstance]
    }

    def update() {
        def hostInstance = Host.get(params.id)
        if (!hostInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'host.label', default: 'Host'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (hostInstance.version > version) {
                hostInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'host.label', default: 'Host')] as Object[],
                          "Another user has updated this Host while you were editing")
                render(view: "edit", model: [hostInstance: hostInstance])
                return
            }
        }

        hostInstance.properties = params

        if (!hostInstance.save(flush: true)) {
            render(view: "edit", model: [hostInstance: hostInstance])
            return
        }

		flash.message = message(code: 'default.updated.message', args: [message(code: 'host.label', default: 'Host'), hostInstance.id])
        redirect(action: "show", id: hostInstance.id)
    }

    def delete() {
        def hostInstance = Host.get(params.id)
        if (!hostInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'host.label', default: 'Host'), params.id])
            redirect(action: "list")
            return
        }
        if (Boolean.valueOf(params.deleteEntity)) {
            try {
                hostInstance.delete(flush: true)
    			flash.message = message(code: 'default.deleted.message', args: [message(code: 'host.label', default: 'Host'), params.id])
                redirect(action: "list")
            }
            catch (DataIntegrityViolationException e) {
    			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'host.label', default: 'Host'), params.id])
                redirect(action: "show", id: params.id)
            }
        } else {
            hostInstance.state = HostState.DELETED;
            hostInstance.save(flush:true)
            redirect(action: "list")
        }
    }

}
