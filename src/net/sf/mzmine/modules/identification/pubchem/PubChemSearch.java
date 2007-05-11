/**
 * 
 */
package net.sf.mzmine.modules.identification.pubchem;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceSoap;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;


/**
 *
 */
public class PubChemSearch implements MZmineModule {

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        try {
            EUtilsServiceLocator service = new EUtilsServiceLocator();
            EUtilsServiceSoap utils = service.geteUtilsServiceSoap();
            // call NCBI ESearch utility
            // NOTE: search term should be URL encoded 
            ESearchRequest req = new ESearchRequest();
            req.setDb("pccompound");
            req.setRetMax("20");
            req.setTerm("391.903:391.913[MonoisotopicMass] AND 1:1000000[CID] NOT Cl[Element] NOT Br[Element]");
            
            
            ESearchResult res = utils.run_eSearch(req);
            // results output
            System.out.println("Found ids: " + res.getCount());
            System.out.print("First " + res.getRetMax() +" ids: ");
            for(int i=0; i<res.getIdList().getId().length; i++)
            {
                System.out.print(res.getIdList().getId()[i] + " ");
            }
            
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Finished ");
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub

    }

}
