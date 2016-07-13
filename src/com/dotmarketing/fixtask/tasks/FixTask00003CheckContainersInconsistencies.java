package com.dotmarketing.fixtask.tasks;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;


public class FixTask00003CheckContainersInconsistencies  implements FixTask {

	private List <Map<String, String>>   modifiedData= new  ArrayList <Map<String,String>>();
	
	public  List <Map <String,Object>> executeFix() throws DotDataException, DotRuntimeException {

		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
		List <Map <String,Object>> returnValue =new ArrayList <Map <String,Object>>();
		int counter = 0;

		final String fix2ContainerQuery = "select c.* from " + Inode.Type.CONTAINERS.getTableName() + " c, inode i where i.inode = c.inode and c.identifier = ? order by mod_date desc";
		final String fix3ContainerQuery = "update container_version_info set working_inode = ? where identifier = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 3: check the working and live versions of containers for inconsistencies");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				String query = "select distinct ident.* " + "from identifier ident, "
						+ "inode i, " + Inode.Type.CONTAINERS.getTableName() + " c "
						+ "where ident.id = c.identifier and "
						+ "ident.id not in (select ident.id "
						+ "from identifier ident, " + "inode i, "
					    + Inode.Type.CONTAINERS.getTableName() + " c, "
					    + "container_version_info cvi "
						+ "where c.identifier = ident.id and "
						+ "i.inode = c.inode and " + "cvi.working_inode = c.inode) and "
						+ "i.type = 'containers' and " + "i.inode = c.inode";
				Logger.debug(CMSMaintenanceFactory.class,
						"Running query for Containers: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> containerIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found "
						+ containerIds.size() + " Containers");
				int total = containerIds.size();

				Logger.info(CMSMaintenanceFactory.class,
						"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

				// Check the working and live versions of containers
				String identifierInode;
				List<HashMap<String, String>> versions;
				HashMap<String, String> version;
				//String versionWorking;
				String DbConnFalseBoolean = DbConnectionFactory.getDBFalse()
						.trim().toLowerCase();

				char DbConnFalseBooleanChar;
				if (DbConnFalseBoolean.charAt(0) == '\'')
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(1);
				else
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(0);

				String inode;

				Logger.info(CMSMaintenanceFactory.class,
						"Verifying working and live versions for "
								+ containerIds.size() + " containers");
				for (HashMap<String, String> identifier : containerIds) {
					identifierInode = identifier.get("id");

					Logger.debug(CMSMaintenanceFactory.class,
							"identifier inode " + identifierInode);
					Logger.debug(CMSMaintenanceFactory.class, "Running query: "
							+ fix2ContainerQuery);

					db.setSQL(fix2ContainerQuery);
					db.addParam(identifierInode);
					versions = db.getResults();
					modifiedData.addAll(versions);

					if (0 < versions.size()) {
						version = versions.get(0);
						//versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class,
								"Non Working Container inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class,
								"Running query: " + fix3ContainerQuery);
						db.setSQL(fix3ContainerQuery);						
						db.addParam(inode);
						db.addParam(identifierInode);
						db.getResult();

						FixAssetsProcessStatus.addAErrorFixed();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}
				getModifiedData();
				FixAudit Audit= new FixAudit();
				Audit.setTableName(Inode.Type.CONTAINERS.getTableName());
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Check the working and live versions of containers for inconsistencies and fix them");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();				
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(CMSMaintenanceFactory.class,
						"Ending fixAssetsInconsistencies");
			} catch (Exception e) {
				Logger.debug(CMSMaintenanceFactory.class,
						"There was a problem fixing asset inconsistencies", e);
				Logger.warn(CMSMaintenanceFactory.class,
						"There was a problem fixing asset inconsistencies", e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}

		 return returnValue;
	}

	
	public List <Map<String, String>> getModifiedData() {

		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdirs();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_"
					+ "FixTask00003CheckContainersInconsistencies" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}
		


	public boolean shouldRun() {
		DotConnect db = new DotConnect();

		String query = "select distinct ident.* " + "from identifier ident, "
				+ "inode i, " + Inode.Type.CONTAINERS.getTableName() + " c "
				+ "where ident.id = c.identifier and "
				+ "ident.id not in (select ident.id " + "from identifier ident, "
				+ "inode i, " + Inode.Type.CONTAINERS.getTableName() + " c, " + "container_version_info cvi "
				+ "where c.identifier = ident.id and "
				+ "i.inode = c.inode and " + "cvi.working_inode = c.inode) and "
				+ "i.type = 'containers' and " + "i.inode = c.inode";

		db.setSQL(query);
		List<HashMap<String, String>> containerIds =null ;
		try {
			containerIds = db.getResults();
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(), e);
		}
		int total = containerIds.size();
		if (total > 0)
			return true;

		else
			return false;
	}

}
