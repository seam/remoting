package org.jboss.seam.remoting.examples.validation.action;

import javax.enterprise.inject.Model;

import org.jboss.seam.remoting.annotations.WebRemote;
import org.jboss.seam.remoting.examples.validation.model.Customer;

/**
 * @author Shane Bryzak
 */
@Model
public class CustomerAction {

    @WebRemote
    public void saveCustomer(Customer customer) {
        // TODO Save the customer
    }
}
